package product.orders.inventoryservice.persistance;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * An event from the order service with a unique id that has been processed
 */
@Entity
@Table(name = "processed_order_event")
public class ProcessedOrderEvent {

    /**
     * Use unique id rather than the eventId because otherwise it will try to merge when an order id is set, but
     * we want to throw an exception if the event has already been processed
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, name = "event_id", unique = true)
    private UUID eventId;

    @Column(nullable = false, updatable = false, name = "processed_at")
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