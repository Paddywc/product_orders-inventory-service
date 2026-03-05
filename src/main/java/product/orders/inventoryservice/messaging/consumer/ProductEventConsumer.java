package product.orders.inventoryservice.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import product.orders.inventoryservice.application.InventoryApplicationService;
import product.orders.inventoryservice.messaging.event.ProductCreatedEvent;
import product.orders.inventoryservice.persistance.ProcessedProductEvent;
import product.orders.inventoryservice.repository.ProcessedProductEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Reads events produced by the product service
 */
@Component
public class ProductEventConsumer {
    private final InventoryApplicationService inventoryApplicationService;

    private final ProcessedProductEventRepository processedProductEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductEventConsumer(InventoryApplicationService inventoryApplicationService, ProcessedProductEventRepository processedProductEventRepository) {
        this.inventoryApplicationService = inventoryApplicationService;
        this.processedProductEventRepository = processedProductEventRepository;
    }

    private boolean isProcessed(UUID eventId) {
        try{
            processedProductEventRepository.saveAndFlush(new ProcessedProductEvent(eventId));
        }catch (DataIntegrityViolationException ex){
            return true;
        }
        return false;
    }

    @KafkaListener(topics = "#{@kafkaTopicsProperties.productEvents}",
                  groupId = "${spring.kafka.consumer.group-id}")
    public void handleEvent(String rawJson,
                            @Header("eventType") String eventType) {
        JsonNode node = objectMapper.readValue(rawJson, JsonNode.class);
        UUID eventId = UUID.fromString(node.get("eventId").stringValue());
        if(isProcessed(eventId)){
            Logger logger = LoggerFactory.getLogger(getClass());
            logger.debug("Event {} already processed", eventId);
            return;
        }

        // Create product if a product created event
        if(eventType.equals("ProductCreatedEvent")){
            ProductCreatedEvent event = objectMapper.treeToValue(node, ProductCreatedEvent.class);
            inventoryApplicationService.addProduct(event.productId(), 0);
        }
    }
    }
