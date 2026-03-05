package product.orders.inventoryservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registry of Kafka topics used in the Inventory Service. Creates a string bean kafkaTopicsProperties
 */
@Configuration
@ConfigurationProperties(prefix = "kafka.topic")
public class KafkaTopicsProperties {

    /**
     * The name of the topic that stores the order service events
     */
    private String orderEvents;
    /**
     * The name of the topic that stores the inventory service events
     */
    private String inventoryEvents;

    /**
     * The name of the topic that stores the product service events
     */
    private String productEvents;

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

    public String getProductEvents() {
        return productEvents;
    }

    public void setProductEvents(String productEvents) {
        this.productEvents = productEvents;
    }
}
