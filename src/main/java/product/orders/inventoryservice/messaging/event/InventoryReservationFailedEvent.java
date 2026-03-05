package product.orders.inventoryservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when inventory reservation fails
 * <p>
 * This event signals a saga failure and should trigger
 * order cancellation and compensation.
 */
public record InventoryReservationFailedEvent(
        UUID eventId,
        UUID orderId,
        InventoryReservationFailedReason reason,
        String failureMessage,
        Instant occurredAt
) {

    public InventoryReservationFailedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
    }

    public static InventoryReservationFailedEvent of(UUID orderId, InventoryReservationFailedReason reason, String failureMessage) {
        return new InventoryReservationFailedEvent(
                UUID.randomUUID(),   // unique event identity
                orderId,
                reason,
                failureMessage,
                Instant.now()
        );
    }
}
