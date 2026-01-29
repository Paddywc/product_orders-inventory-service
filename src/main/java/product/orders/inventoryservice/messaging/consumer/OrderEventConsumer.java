package product.orders.inventoryservice.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import product.orders.inventoryservice.application.InventorySagaHandler;
import product.orders.inventoryservice.messaging.event.OrderCancelledEvent;
import product.orders.inventoryservice.messaging.event.OrderConfirmedEvent;
import product.orders.inventoryservice.messaging.event.OrderCreatedEvent;
import product.orders.inventoryservice.persistance.ProcessedOrderEvent;
import product.orders.inventoryservice.repository.ProcessedOrderEventRepository;

import java.util.UUID;

@Component

public class OrderEventConsumer {


    private final InventorySagaHandler sagaHandler;
    private final ProcessedOrderEventRepository processedOrderEventRepository;

    public OrderEventConsumer(InventorySagaHandler sagaHandler, ProcessedOrderEventRepository processedOrderEventRepository) {
        this.sagaHandler = sagaHandler;
        this.processedOrderEventRepository = processedOrderEventRepository;
    }

    /**
     * Handle incoming order events
     * @param event the send order event
     */
    @KafkaListener(topics = "#{@kafkaTopicsProperties.orderEvents}",
            groupId = "${kafka.consumer.group-id}")
    public void handle(Object event){
        if (event instanceof OrderCreatedEvent e) {
            handle(e.eventId(), () -> sagaHandler.reserveInventory(e));
        }

        else if (event instanceof OrderCancelledEvent e) {
            handle(e.eventId(), () -> sagaHandler.releaseInventory(e));
        }

        else if (event instanceof OrderConfirmedEvent e) {
            handle(e.eventId(), () -> sagaHandler.confirmInventory(e));
        }

        else{
            throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
        }
    }


    /**
     * Idempotent execution that checks if the event has already been processsed
     * @param eventId the id of the event
     * @param action the action to perform if the event has not been processed
     */
    private void handle(UUID eventId, Runnable action){
        if (processedOrderEventRepository.existsById(eventId)) {
            return;
        }
        action.run();
        processedOrderEventRepository.save(new ProcessedOrderEvent(eventId));
    }

}
