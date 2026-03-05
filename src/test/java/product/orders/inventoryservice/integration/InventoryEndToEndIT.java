package product.orders.inventoryservice.integration;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;
import product.orders.inventoryservice.config.KafkaTopicsProperties;
import product.orders.inventoryservice.domain.model.InventoryReservation;
import product.orders.inventoryservice.domain.model.Product;
import product.orders.inventoryservice.domain.model.ReservationStatus;
import product.orders.inventoryservice.messaging.event.*;
import product.orders.inventoryservice.repository.InventoryReservationRepository;
import product.orders.inventoryservice.repository.ProductRepository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
class InventoryEndToEndIT {
    // ---------- Containers ----------
    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("paymentdb")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
            );


    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setupConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        testConsumer = new KafkaConsumer<>(props);
        testConsumer.subscribe(List.of(topics.getOrderEvents(), topics.getInventoryEvents()));
    }

    @AfterEach
    void tearDown() {
        testConsumer.close();
    }

    // ---------- Beans ----------
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;


    private Consumer<String, String> testConsumer;


    @Autowired
    private KafkaTopicsProperties topics;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;


    // ---------- Tests ----------
    @Test
    void testAddProduct_ConsumesProductCreatedEvent_AddsProductWithZeroStock() {
        // Arrange
        UUID productId = UUID.randomUUID();
        ProductCreatedEvent event = new ProductCreatedEvent(UUID.randomUUID(), productId);
        Message<ProductCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topics.getProductEvents())
                .setHeader("eventType", "ProductCreatedEvent")
                .build();

        // Act
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return null;
        });

        // Assert
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Optional<Product> result = productRepository.findById(productId);
                    assertTrue(result.isPresent());
                    Product product = result.get();
                    assertEquals(productId, product.getId());
                    assertEquals(0, product.getAvailableQuantity());
                });
    }

    @Test
    void testReserveInventory_ConsumesOrderCreatedEvent_ReservesAllItemsAndPublishesEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productOneId = UUID.randomUUID();
        UUID productTwoId = UUID.randomUUID();
        Product productOne = new Product(productOneId, 20);
        Product productTwo = new Product(productTwoId, 50);
        productRepository.saveAll(List.of(productOne, productTwo));

        OrderCreatedEvent event = OrderCreatedEvent.of(
                orderId,
                1000L,
                "USD",
                UUID.randomUUID(),
                "example@email.com",
                "123 Fake st",
                List.of(new OrderItem(productOneId, 10), new OrderItem(productTwoId, 5))
        );
        Message<OrderCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topics.getOrderEvents())
                .setHeader("eventType", "OrderCreatedEvent")
                .build();

        // Act
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return null;
        });

        // Assert
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Optional<Product> result = productRepository.findById(productOneId);
                    assertTrue(result.isPresent());
                    Product product = result.get();
                    assertEquals(10, product.getAvailableQuantity());

                    result = productRepository.findById(productTwoId);
                    assertTrue(result.isPresent());
                    product = result.get();
                    assertEquals(45, product.getAvailableQuantity());

                    assertTrue(recordsContainEventOfType("InventoryReservedEvent"));
                });

    }

    @Test
    void testReleaseInventory_ConsumesOrderCancelledEvent_ReleasesAllItemsAndPublishesEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productOneId = UUID.randomUUID();
        UUID productTwoId = UUID.randomUUID();
        Product productOne = new Product(productOneId, 20);
        Product productTwo = new Product(productTwoId, 50);
        productRepository.saveAll(List.of(productOne, productTwo));

        InventoryReservation reservationOne = InventoryReservation.reserve(orderId, productOneId, 10);
        inventoryReservationRepository.save(reservationOne);
        productOne.reserve(10);
        productRepository.save(productOne);

        InventoryReservation reservationTwo = InventoryReservation.reserve(orderId, productTwoId, 10);
        inventoryReservationRepository.save(reservationTwo);
        productTwo.reserve(10);
        productRepository.save(productTwo);


        OrderCancelledEvent event = OrderCancelledEvent.of(orderId, CancellationReason.PAYMENT_FAILED);
        Message<OrderCancelledEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topics.getOrderEvents())
                .setHeader("eventType", "OrderCancelledEvent")
                .build();

        // Act
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return null;
        });

        // Assert
        Awaitility
                .await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Optional<Product> result = productRepository.findById(productOneId);
                    assertTrue(result.isPresent());
                    Product product = result.get();
                    assertEquals(20, product.getAvailableQuantity());

                    InventoryReservation updatedReservation = inventoryReservationRepository.findByOrderIdAndProductId(orderId, productOneId);
                    assertEquals(ReservationStatus.RELEASED, updatedReservation.getStatus());

                    updatedReservation = inventoryReservationRepository.findByOrderIdAndProductId(orderId, productTwoId);
                    assertEquals(ReservationStatus.RELEASED, updatedReservation.getStatus());

                    assertTrue(recordsContainEventOfType("InventoryReleasedEvent"));
                });
    }

    @Test
    void testConfirmInventory_ConsumesOrderConfirmedEvent_ConfirmsAllInventoryAndPublishesEvent(){
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productOneId = UUID.randomUUID();
        UUID productTwoId = UUID.randomUUID();
        Product productOne = new Product(productOneId, 20);
        Product productTwo = new Product(productTwoId, 50);
        productRepository.saveAll(List.of(productOne, productTwo));

        InventoryReservation reservationOne = InventoryReservation.reserve(orderId, productOneId, 10);
        inventoryReservationRepository.save(reservationOne);
        productOne.reserve(10);
        productRepository.save(productOne);

        InventoryReservation reservationTwo = InventoryReservation.reserve(orderId, productTwoId, 10);
        inventoryReservationRepository.save(reservationTwo);
        productTwo.reserve(10);
        productRepository.save(productTwo);

        OrderConfirmedEvent event = OrderConfirmedEvent.of(orderId);
        Message<OrderConfirmedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topics.getOrderEvents())
                .setHeader("eventType", "OrderConfirmedEvent")
                .build();

        // Act
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return null;
        });

        // Assert
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    InventoryReservation updatedReservation = inventoryReservationRepository.findByOrderIdAndProductId(orderId, productOneId);
                    assertEquals(ReservationStatus.CONFIRMED, updatedReservation.getStatus());

                    updatedReservation = inventoryReservationRepository.findByOrderIdAndProductId(orderId, productTwoId);
                    assertEquals(ReservationStatus.CONFIRMED, updatedReservation.getStatus());

                    assertTrue(recordsContainEventOfType("InventoryConfirmedEvent"));
                });

    }

    // ---------- Helpers ----------

    private boolean recordsContainEventOfType(String eventType) {
        ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofMillis(100));
        boolean found = false;

        for (ConsumerRecord<String, String> record : records) {
            // Convert from byte[] to String
            String headerEventType = new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
            if (headerEventType.equals(eventType)) {
                found = true;
                break;
            }

        }

        return found;
    }

}
