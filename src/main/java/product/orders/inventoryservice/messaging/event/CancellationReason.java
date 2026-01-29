package product.orders.inventoryservice.messaging.event;

public enum CancellationReason {
    INVENTORY_INSUFFICIENT,
    PAYMENT_FAILED,
    USER_CANCELLED,
    TIMEOUT,
    SYSTEM_ERROR
}
