package product.orders.inventoryservice.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An order event with a unique id that has been processed
 */
@Entity
@Table(name = "processed_events")
public class ProcessedOrderEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedOrderEvent() {
        // JPA only
    }

    public ProcessedOrderEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}