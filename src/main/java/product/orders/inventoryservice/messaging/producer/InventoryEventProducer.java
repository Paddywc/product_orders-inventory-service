package product.orders.inventoryservice.messaging.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import product.orders.inventoryservice.config.KafkaTopicsProperties;
import product.orders.inventoryservice.messaging.event.InventoryConfirmedEvent;
import product.orders.inventoryservice.messaging.event.InventoryReleasedEvent;
import product.orders.inventoryservice.messaging.event.InventoryReservationFailedEvent;
import product.orders.inventoryservice.messaging.event.InventoryReservedEvent;

import java.util.UUID;

@Component
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private KafkaTopicsProperties topics;


    public InventoryEventProducer(KafkaTemplate<String, Object> kafkaTemplate, KafkaTopicsProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    private String getTopicName() {
        return topics.getInventoryEvents();
    }

    private <T> Message<T> buildMessage(T event, UUID orderId, String eventType) {
        return MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, getTopicName())
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .setHeader("eventType", eventType)
                .setHeader("orderId", orderId.toString())
                .build();
    }

    @Transactional(value = "kafkaTransactionManager")
    public void publish(InventoryReservedEvent event) {
        Message<InventoryReservedEvent> message = buildMessage(event, event.orderId(), "InventoryReservedEvent");
        kafkaTemplate.send(message);
    }

    @Transactional(value = "kafkaTransactionManager")
    public void publish(InventoryReservationFailedEvent event) {
        Message<InventoryReservationFailedEvent> message = buildMessage(event, event.orderId(), "InventoryReservationFailedEvent");
        kafkaTemplate.send(message);
    }

    @Transactional(value = "kafkaTransactionManager")
    public void publish(InventoryConfirmedEvent event) {
        Message<InventoryConfirmedEvent> message = buildMessage(event, event.orderId(), "InventoryConfirmedEvent");
        kafkaTemplate.send(message);
    }

    @Transactional(value = "kafkaTransactionManager")
    public void publish(InventoryReleasedEvent event) {
        Message<InventoryReleasedEvent> message = buildMessage(event, event.orderId(), "InventoryReleasedEvent");
        kafkaTemplate.send(message);
    }


}
