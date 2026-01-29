package product.orders.inventoryservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when inventory has been successfully reserved
 * for all items in an order.
 *
 * This event is outside the Saga and indicates that the Inventory Service has
 * completed its confirmation successfully
 */
public record InventoryConfirmedEvent(
        UUID eventId,
        UUID orderId,
        Instant occurredAt
) {

    public InventoryConfirmedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
    }

    public static InventoryConfirmedEvent of(UUID orderId) {
        return new InventoryConfirmedEvent(
                UUID.randomUUID(),   // unique event identity
                orderId,
                Instant.now()
        );
    }
}
