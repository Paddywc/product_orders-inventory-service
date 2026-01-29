package product.orders.inventoryservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when inventory has been successfully reserved
 * for all items in an order.
 *
 * This event indicates that the Inventory Service has
 * completed its saga step successfully.
 */
public record InventoryReservedEvent(
        UUID eventId,
        UUID orderId,
        Instant occurredAt
) {

    public InventoryReservedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
    }

    public static InventoryReservedEvent of(UUID orderId) {
        return new InventoryReservedEvent(
                UUID.randomUUID(),   // unique event identity
                orderId,
                Instant.now()
        );
    }
}
