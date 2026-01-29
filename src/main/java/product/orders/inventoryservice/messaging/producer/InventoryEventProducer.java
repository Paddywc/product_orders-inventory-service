package product.orders.inventoryservice.messaging.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import product.orders.inventoryservice.messaging.event.InventoryConfirmedEvent;
import product.orders.inventoryservice.messaging.event.InventoryInsufficientEvent;
import product.orders.inventoryservice.messaging.event.InventoryReleasedEvent;
import product.orders.inventoryservice.messaging.event.InventoryReservedEvent;

@Component
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    /**
     * The name of the Kafka topic as per the settings
     */
    private final String topicName;


    public InventoryEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                                  @Value("#{@kafkaTopicsProperties.inventoryEvents}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publish(InventoryReservedEvent event) {
        kafkaTemplate.send(
                topicName,
                event.orderId().toString(), // Order by orderId
                event
        );
    }

    public void publish(InventoryInsufficientEvent event) {
        kafkaTemplate.send(
                topicName,
                event.orderId().toString(), // Order by orderId
                event
        );
    }

    public void publish(InventoryConfirmedEvent event) {
        kafkaTemplate.send(
                topicName,
                event.orderId().toString(), // Order by orderId
                event
        );
    }

    public void publish(InventoryReleasedEvent event) {
        kafkaTemplate.send(
                topicName,
                event.orderId().toString(), // Order by orderId
                event
        );
    }


}
