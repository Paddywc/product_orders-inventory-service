package product.orders.inventoryservice.messaging.event;


import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when previously reserved inventory has been released
 * as part of saga compensation.
 *
 * This event indicates that Inventory has successfully undone
 * its reservation work for the given order.
 */
public record InventoryReleasedEvent(
        UUID eventId,
        UUID orderId,
        Instant occurredAt
) {

    public InventoryReleasedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
    }

    public static InventoryReleasedEvent of(UUID orderId) {
        return new InventoryReleasedEvent(
                UUID.randomUUID(),   // unique event identity
                orderId,
                Instant.now()
        );
    }
}