package product.orders.inventoryservice.application.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import product.orders.inventoryservice.application.InventoryApplicationService;
import product.orders.inventoryservice.domain.exception.InsufficientInventoryException;
import product.orders.inventoryservice.domain.exception.NoProductFoundException;
import product.orders.inventoryservice.messaging.event.*;
import product.orders.inventoryservice.messaging.producer.InventoryEventProducer;

@Service
public class InventorySagaHandler {

    private final InventoryApplicationService inventoryApplicationService;

    private final InventoryEventProducer eventProducer;

    private final InventoryReservationTxService inventoryReservationTxService;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    public InventorySagaHandler(InventoryApplicationService inventoryApplicationService,
                                InventoryEventProducer eventProducer,
                                InventoryReservationTxService inventoryReservationTxService) {
        this.inventoryApplicationService = inventoryApplicationService;
        this.eventProducer = eventProducer;
        this.inventoryReservationTxService = inventoryReservationTxService;
    }


    /**
     * Reserve inventory for all items in an order, handling exceptions and publishing events. Publishes
     * InventoryReservedEvent on success. Publishes InventoryReservationFailedEvent on failure.
     * @param event the order whose items need to be reserved
     */
    public void reserveInventory(OrderCreatedEvent event) {
        try {
            inventoryReservationTxService.reserveAllItemsOrThrow(event);
            eventProducer.publish(InventoryReservedEvent.of(event.orderId()));
        } catch (InsufficientInventoryException ex) {
            String message = "Attempted to reserve " + ex.getRequestedQuantity() + " units of product " + ex.getProductId() + " but only " + ex.getAvailableQuantity() + " units are available";
            logger.debug(message);
            eventProducer.publish(
                    InventoryReservationFailedEvent.of(
                            event.orderId(),
                            InventoryReservationFailedReason.INSUFFICIENT_INVENTORY,
                            message
                    )
            );

        }catch (NoProductFoundException ex){
            logger.debug("Missing product for order: {}. Product details: {}", event.orderId(), ex.getMessage());
            eventProducer.publish(
                    InventoryReservationFailedEvent.of(
                            event.orderId(),
                            InventoryReservationFailedReason.SERVER_DATA_ERROR,
                            ex.getMessage()));
        }
        catch (IllegalArgumentException ex) { // Thrown when an invalid quantity is given
            logger.debug("Invalid request for reservation: {}", ex.getMessage());
            eventProducer.publish(
                    InventoryReservationFailedEvent.of(
                            event.orderId(),
                            InventoryReservationFailedReason.INVALID_REQUEST,
                            ex.getMessage())
            );
        } catch (DataIntegrityViolationException ex) {// Thrown when an inventory reservation already exists
            logger.debug("Duplicate reservation attempt for order: {}", event.orderId());
            eventProducer.publish(
                    InventoryReservationFailedEvent.of(
                            event.orderId(),
                            InventoryReservationFailedReason.DUPLICATE_RESERVATION,
                            ex.getMessage())
            );
        }catch (Exception ex){
            logger.error("Unexpected error occurred while reserving inventory for order: {}", event.orderId(), ex);
            throw ex;
        }
    }

    /**
     * Release inventory for all items in an order. Publishes InventoryReleasedEvent on success. Handles exceptions.
     * @param event the order whose items need to be released
     */
    public void releaseInventory(OrderCancelledEvent event) {
        try {
            inventoryApplicationService.release(event.orderId());
            eventProducer.publish(InventoryReleasedEvent.of(event.orderId()));
        } catch (IllegalStateException ex) { // Thrown when attempting to release a confirmed reservation
            logger.debug("Failed to release inventory for order {}: {}", event.orderId(), ex.getMessage());
        } catch (IllegalArgumentException ex) { // Thrown if a negative quantity is given
            logger.debug("Invalid request for inventory release: {}", ex.getMessage());
        }
    }

    /**
     * Confirm inventory for all items in an order. Publishes InventoryConfirmedEvent on success. Handles exceptions.
     * Publishes InventoryReservationFailedEvent on failure.
     * @param event the order whose items need to be confirmed
     */
    public void confirmInventory(OrderConfirmedEvent event) {
        try {
            inventoryApplicationService.confirm(event.orderId());
            eventProducer.publish(InventoryConfirmedEvent.of(event.orderId()));
        } catch (IllegalStateException ex) { // Thrown when attempting to confirm a released reservation
            logger.debug("Failed to confirm inventory for order {}: {}", event.orderId(), ex.getMessage());
            eventProducer.publish(
                    InventoryReservationFailedEvent.of(
                            event.orderId(),
                            InventoryReservationFailedReason.ILLEGAL_RESERVATION_STATE,
                            ex.getMessage())
            );
        }
    }

}
