package product.orders.inventoryservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an order has been fully processed and confirmed.

 * This event represents the successful completion of the order saga.
 * Downstream services should treat this as a terminal success fact.
 */
public record OrderConfirmedEvent(

        UUID eventId,

        UUID orderId,


        Instant occurredAt) {

    public OrderConfirmedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
    }

    public static OrderConfirmedEvent of(UUID orderId) {
        return new OrderConfirmedEvent(
                UUID.randomUUID(),   // event identity
                orderId,
                Instant.now());
    }
}