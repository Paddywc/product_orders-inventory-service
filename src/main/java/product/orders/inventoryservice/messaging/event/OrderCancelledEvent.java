package product.orders.inventoryservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an order is cancelled.

 * This event is terminal for the order saga and is used
 * by downstream services (e.g. Inventory) to perform
 * compensation actions.
 */

public record OrderCancelledEvent(
        /**
         * Unique event id. Prevent duplication
         */
        UUID eventId,

        /**
         * Unique id of the order that was canceled
         */
        UUID orderId,

        /**
         * Time the order was cancelled
         */
        Instant occurredAt,

        /**
         * The reason why the order was cancelled
         */
        CancellationReason reason) {

    public OrderCancelledEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        if (reason == null) {
            throw new IllegalArgumentException("cancellation reason must not be null");
        }
    }

    public static OrderCancelledEvent of(UUID orderId, CancellationReason reason) {
        return new OrderCancelledEvent(
                UUID.randomUUID(),   // event identity
                orderId,
                Instant.now(),
                reason);
    }
}