package product.orders.inventoryservice.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import product.orders.inventoryservice.application.saga.InventorySagaHandler;
import product.orders.inventoryservice.messaging.event.OrderCancelledEvent;
import product.orders.inventoryservice.messaging.event.OrderConfirmedEvent;
import product.orders.inventoryservice.messaging.event.OrderCreatedEvent;
import product.orders.inventoryservice.persistance.ProcessedOrderEvent;
import product.orders.inventoryservice.repository.ProcessedOrderEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Reads events produced by the order service
 */
@Component
public class OrderEventConsumer {


    private final InventorySagaHandler sagaHandler;
    private final ProcessedOrderEventRepository processedOrderEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderEventConsumer(InventorySagaHandler sagaHandler, ProcessedOrderEventRepository processedOrderEventRepository) {
        this.sagaHandler = sagaHandler;
        this.processedOrderEventRepository = processedOrderEventRepository;
    }

    @KafkaListener(topics = "#{@kafkaTopicsProperties.orderEvents}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void handleEvent(String rawJson,
                            @Header("eventType") String eventType) {
        JsonNode node = objectMapper.readValue(rawJson, JsonNode.class);
        UUID eventId = UUID.fromString(node.get("eventId").stringValue());

        // Do not process duplicate events
        try {
            processedOrderEventRepository.saveAndFlush(new ProcessedOrderEvent(eventId));
        } catch (DataIntegrityViolationException e) {
            // Event already exists. Exit. Catch exception rather than checking to
            // avoid idempotency issues
            Logger logger = LoggerFactory.getLogger(getClass());
            logger.debug("Event {} already processed", eventId);
            return;
        }

        switch (eventType) {
            case "OrderCreatedEvent" ->
                    sagaHandler.reserveInventory(objectMapper.treeToValue(node, OrderCreatedEvent.class));
            case "OrderCancelledEvent" ->
                    sagaHandler.releaseInventory(objectMapper.treeToValue(node, OrderCancelledEvent.class));
            case "OrderConfirmedEvent" ->
                    sagaHandler.confirmInventory(objectMapper.treeToValue(node, OrderConfirmedEvent.class));
        }

    }

}
