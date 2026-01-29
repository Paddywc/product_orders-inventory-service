package product.orders.inventoryservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when inventory reservation fails due to insufficient stock.

 * This event signals a saga failure and should trigger
 * order cancellation and compensation.
 */
public record InventoryInsufficientEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        int requestedQuantity,
        int availableQuantity,
        Instant occurredAt
) {

    public InventoryInsufficientEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("requestedQuantity must be positive");
        }
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("availableQuantity must not be negative");
        }
    }

    public static InventoryInsufficientEvent of(
            UUID orderId,
            UUID productId,
            int requestedQuantity,
            int availableQuantity
    ) {
        return new InventoryInsufficientEvent(
                UUID.randomUUID(),   // unique event identity
                orderId,
                productId,
                requestedQuantity,
                availableQuantity,
                Instant.now()
        );
    }
}