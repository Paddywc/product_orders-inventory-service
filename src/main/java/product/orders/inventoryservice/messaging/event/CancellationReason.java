package product.orders.inventoryservice.messaging.event;

/**
 * Reasons why an {@link OrderCancelledEvent} can be emitted.
 */
public enum CancellationReason {
    INVENTORY_RESERVATION_FAILED,
    PAYMENT_FAILED,
    ORDER_NOT_FOUND,
    USER_CANCELLED,
    TIMEOUT,
    SYSTEM_ERROR
}
