package product.orders.inventoryservice.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.orders.inventoryservice.domain.exception.InsufficientInventoryException;
import product.orders.inventoryservice.domain.service.InventoryService;
import product.orders.inventoryservice.messaging.event.*;
import product.orders.inventoryservice.messaging.producer.InventoryEventProducer;

@Service
public class InventorySagaHandler {

    private final InventoryService inventoryService;

    private final InventoryEventProducer eventProducer;


    public InventorySagaHandler(InventoryService inventoryService, InventoryEventProducer eventProducer) {
        this.inventoryService = inventoryService;
        this.eventProducer = eventProducer;
    }

    /**
     * Reserve inventory for all items in an order
     * @param event the order whose items need to be reserved
     */
    @Transactional
    public void reserveInventory(OrderCreatedEvent event){
        for(OrderItem orderItem: event.items()){
            try{
                inventoryService.reserve(event.orderId(), orderItem.productId(), orderItem.quantity());
            }catch(InsufficientInventoryException ex){
                eventProducer.publish(
                        InventoryInsufficientEvent.of(
                                event.orderId(),
                                orderItem.productId(),
                                orderItem.quantity(),
                                inventoryService.getAvailableQuantity(orderItem.productId())
                        )
                );
            }
        }
    }

    @Transactional
    public void releaseInventory(OrderCancelledEvent event){
        inventoryService.release(event.orderId());
        eventProducer.publish(InventoryReleasedEvent.of(event.orderId()));
    }

    @Transactional
    public void confirmInventory(OrderConfirmedEvent event){
        inventoryService.confirm(event.orderId());
        eventProducer.publish(InventoryConfirmedEvent.of(event.orderId()));
    }

}
