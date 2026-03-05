package product.orders.inventoryservice.messaging.consumer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.testcontainers.utility.DockerImageName;
import product.orders.inventoryservice.config.KafkaTopicsProperties;
import product.orders.inventoryservice.domain.model.InventoryReservation;
import product.orders.inventoryservice.domain.model.Product;
import product.orders.inventoryservice.domain.model.ReservationStatus;
import product.orders.inventoryservice.messaging.event.*;
import product.orders.inventoryservice.repository.InventoryReservationRepository;
import product.orders.inventoryservice.repository.ProductRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class InventoryConsumerIT {

    // ---------------------------------------------------------
    // Containers
    // ---------------------------------------------------------

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("inventory_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers);

        registry.add("spring.datasource.url",
                mysql::getJdbcUrl);

        registry.add("spring.datasource.username",
                mysql::getUsername);

        registry.add("spring.datasource.password",
                mysql::getPassword);
    }

    static {
        mysql.start();
        kafka.start();
    }

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @AfterAll
    static void tearDown() {
        mysql.stop();
        kafka.stop();
    }

    // ---------------------------------------------------------
    // Beans
    // ---------------------------------------------------------

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    KafkaTopicsProperties topics;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;


    @Test
    void testConsumer_WhenOrderCreatedEventFires_ReducesItemInventory() {

        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = new Product(productId, 10);
        productRepository.saveAndFlush(product);

        OrderCreatedEvent event = OrderCreatedEvent.of(orderId,
                1500L,
                "EUR",
                UUID.randomUUID(),
                "example@gmail.com",
                "123 Fake st",
                List.of(new OrderItem(productId, 3)));
        Message<OrderCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topics.getOrderEvents())
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .setHeader("eventType", "OrderCreatedEvent")
                .build();


        // Act
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return null;
        });

        // Assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Product updatedProduct = productRepository.findById(productId).orElseThrow();
            assertThat(updatedProduct.getAvailableQuantity()).isEqualTo(7);
        });

    }

    @Test
    void testConsumer_MultipleOrderCreatedEventsFired_OnlyProcessedOnce() {

        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = new Product(productId, 8);
        productRepository.save(product);

        OrderCreatedEvent event = OrderCreatedEvent.of(orderId,
                500L,
                "USD",
                UUID.randomUUID(),
                "customeremail@example.com",
                "123 Fake st",
                List.of(new OrderItem(productId, 2)));
        Message<OrderCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topics.getOrderEvents())
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .setHeader("eventType", "OrderCreatedEvent")
                .build();

        // Act
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return true;
        });
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return true;
        });
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return true;
        });


        // Assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Product updatedProduct = productRepository.findById(productId).orElseThrow();
            assertThat(updatedProduct.getAvailableQuantity()).isEqualTo(6);
        });
    }

    @Test
    void testConsumer_OrderCreatedForMoreThanAvailableStock_NoStockDecrease() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = new Product(productId, 3);
        productRepository.save(product);

        OrderCreatedEvent event = OrderCreatedEvent.of(orderId,
                1500L,
                "USD",
                UUID.randomUUID(),
                "customeremail@example.com",
                "123 Fake st",
                List.of(new OrderItem(productId, 5)));
        Message<OrderCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topics.getOrderEvents())
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .setHeader("eventType", "OrderCreatedEvent")
                .build();


        // Act
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return true;
        });

        // Assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Product updatedProduct = productRepository.findById(productId).orElseThrow();
            assertThat(updatedProduct.getAvailableQuantity()).isEqualTo(3);
        });
    }


    @Test
    void testConsumer_OrderCancelledEventFires_ReturnsReservedInventory() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int startingQuantity = 6;
        int reservedQuantity = 2;

        Product product = new Product(productId, startingQuantity);
        product.reserve(reservedQuantity);
        productRepository.save(product);

        InventoryReservation inventoryReservation = InventoryReservation.reserve(orderId, productId, reservedQuantity);
        inventoryReservationRepository.save(inventoryReservation);

        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(),
                orderId,
                CancellationReason.USER_CANCELLED,
                Instant.now());
        Message<OrderCancelledEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topics.getOrderEvents())
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .setHeader("eventType", "OrderCancelledEvent")
                .build();


        // Act
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return null;
        });

        // Assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Product updatedProduct = productRepository.findById(productId).orElseThrow();
            assertThat(updatedProduct.getAvailableQuantity()).isEqualTo(startingQuantity);
        });

    }

    @Test
    void testConsumer_OrderConfirmedEventFires_ConfirmsReservation() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Product product =
                new Product(productId, 10);
        productRepository.save(product);

        InventoryReservation inventoryReservation = InventoryReservation.reserve(orderId, productId, 4);
        inventoryReservationRepository.save(inventoryReservation);

        // Act
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                UUID.randomUUID(),
                orderId,
                Instant.now()
        );
        Message<OrderConfirmedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topics.getOrderEvents())
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .setHeader("eventType", "OrderConfirmedEvent")
                .build();

        // Act
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return null;
        });

        // Asset
        // Assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            InventoryReservation updatedReservation = inventoryReservationRepository.findByOrderIdAndProductId(
                    orderId,
                    productId
            );
            assertThat(updatedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        });
    }

    @Test
    void testConsumer_ProductCreatedEventFires_SavesProductWithZeroStock(){
        // Arrange
        UUID productId = UUID.randomUUID();
        ProductCreatedEvent event = new ProductCreatedEvent(UUID.randomUUID(), productId);
        Message<ProductCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topics.getProductEvents())
                .setHeader(KafkaHeaders.KEY, productId.toString())
                .setHeader("eventType", "ProductCreatedEvent")
                .build();

        // Act
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return null;
        });

        // Assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Product product = productRepository.findById(productId).orElseThrow();
            assertNotNull(product);
            assertThat(product.getAvailableQuantity()).isEqualTo(0);
        });
    }
}
