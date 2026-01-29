package product.orders.inventoryservice.messaging.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Emitted when an order is successfully created and
 * the order saga should begin.
 * This event is immutable and safe for replay.
 */
public record OrderCreatedEvent(
        /**
         * Unique event id. Prevent duplication
         */
        UUID eventId,

        /**
         * Unique id of the order that was created
         */
        UUID orderId,

        /**
         * Time order occurred
         */
        Instant occurredAt,
        /**
         * Items that were created
         */
        List<OrderItem> items) {

    public OrderCreatedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
    }

    public static OrderCreatedEvent of(UUID orderId, List<OrderItem> items) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),   // event identity
                orderId,
                Instant.now(),
                List.copyOf(items)   // defensive copy
        );
    }
}