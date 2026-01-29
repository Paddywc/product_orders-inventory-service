package product.orders.inventoryservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registry of Kafka topics used in the Inventory Service. Creates a string bean kafkaTopicsProperties
 */
@Configuration
@ConfigurationProperties(prefix = "kafka.topic")
public class KafkaTopicsProperties {

    private String orderEvents;
    private String inventoryEvents;

    public String getOrderEvents() {
        return orderEvents;
    }

    public void setOrderEvents(String orderEvents) {
        this.orderEvents = orderEvents;
    }

    public String getInventoryEvents() {
        return inventoryEvents;
    }

    public void setInventoryEvents(String inventoryEvents) {
        this.inventoryEvents = inventoryEvents;
    }
}
