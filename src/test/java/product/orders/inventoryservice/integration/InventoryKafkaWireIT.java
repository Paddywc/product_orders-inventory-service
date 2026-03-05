package product.orders.inventoryservice.integration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.json.JsonMapper;
import org.testcontainers.utility.DockerImageName;
import product.orders.inventoryservice.config.KafkaTopicsProperties;
import product.orders.inventoryservice.domain.model.Product;
import product.orders.inventoryservice.messaging.event.OrderCreatedEvent;
import product.orders.inventoryservice.messaging.event.OrderItem;
import product.orders.inventoryservice.repository.ProductRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext
@Testcontainers
class InventoryKafkaWireIT {

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
            );

    static {
        kafka.start();
    }

    @DynamicPropertySource
    static void overrideKafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @AfterAll
    static void tearDown() {
        if (kafka != null) {
            kafka.stop();
        }
    }



    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    @Autowired
    private KafkaTopicsProperties topics;


    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testKafkaWire_OrderCreatedEventFires_ReadableConsumerRecord() {
        UUID orderId = UUID.randomUUID();
        UUID itemUUID = UUID.randomUUID();
        int quantity = 4;

        Product product = new Product(itemUUID, 10);
        productRepository.save(product);
        OrderCreatedEvent event = OrderCreatedEvent.of(orderId, 1500L, "EUR", UUID.randomUUID(), "example@gmail.com", "123 Fake st", List.of(new OrderItem(itemUUID, quantity)));

        // Create a dedicated consumer group for assertions (no competition with app listeners)
        String groupId = "uq-test-consumer-group-" + UUID.randomUUID();
        Consumer<String, Object> consumer = consumerFactory.createConsumer(groupId, UUID.randomUUID().toString());

        consumer.subscribe(List.of(topics.getOrderEvents()));


        // Ensure the consumer actually joins the group and gets partition assignments
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            consumer.poll(Duration.ofMillis(100));
            Set<TopicPartition> assignment = consumer.assignment();
            assertThat(assignment.isEmpty()).isFalse();
        });

        Message<OrderCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topics.getOrderEvents())
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .setHeader("eventType", "OrderCreatedEvent")
                .build();

        // Act
        // (send within a Kafka transaction if the producer is transaction-capable)
        kafkaTemplate.executeInTransaction(kt -> {
            kt.send(message);
            kt.flush();
            return null; // return value is ignored
        });


        // Assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(500));
            assertThat(records.count()).isEqualTo(1);

            ConsumerRecord<String, Object> record = records.iterator().next();
            assertThat(record.key()).isEqualTo(orderId.toString());

            OrderCreatedEvent consumedEvent = objectMapper.readValue(record.value().toString(), OrderCreatedEvent.class);
            assertThat(consumedEvent).isNotNull();
            assertEquals(orderId, consumedEvent.orderId());
            assertEquals(itemUUID, consumedEvent.items().iterator().next().productId());
        });
    }
}
