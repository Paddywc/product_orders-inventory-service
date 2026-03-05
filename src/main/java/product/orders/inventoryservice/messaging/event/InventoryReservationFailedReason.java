package product.orders.inventoryservice.messaging.event;

/**
 * Reasons why an {@link InventoryReservationFailedEvent} can be emitted.
 */
public enum InventoryReservationFailedReason {
   INSUFFICIENT_INVENTORY,
   SERVER_DATA_ERROR,
   INVALID_REQUEST,
   DUPLICATE_RESERVATION,
   ILLEGAL_RESERVATION_STATE
}
