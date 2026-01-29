package product.orders.inventoryservice.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservation", uniqueConstraints = {
        // Enforce idempotency by only allowing 1 reservation per order and product
        @UniqueConstraint(name = "uk_inventory_reservation_order_product",columnNames = {"order_id", "product_id"})})
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID reservationId;

    @Column(nullable = false, updatable = false)
    private UUID orderId;

    @Column(nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    protected InventoryReservation() {
        // JPA only
    }

    private InventoryReservation(UUID reservationId, UUID orderId, UUID productId, int quantity, ReservationStatus status) {
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /* ---------- Getters ---------- */

    public UUID getReservationId() {
        return reservationId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
    /* ---------- Factory methods ---------- */

    /**
     * Factory method for creating a new reservation
     * @param orderId the id of the order that the item is being reserved on
     * @param productId the product being reserved in the order
     * @param quantity the quantity of the product to reserve
     * @return
     */
    public static InventoryReservation reserve(UUID orderId, UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive");
        }

        return new InventoryReservation(UUID.randomUUID(), orderId, productId, quantity, ReservationStatus.RESERVED);
    }

    /* ---------- State transitions ---------- */

    /**
     * Set the reservation status to released. Throw an exception if in the confirmed status state
     */
    public void release() {
        if (status == ReservationStatus.RELEASED) {
            return; // idempotent
        }
        if (status == ReservationStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Cannot release a confirmed reservation"
            );
        }
        this.status = ReservationStatus.RELEASED;
        this.updatedAt = Instant.now();
    }

    /**
     * Sets the status to confirmed. Throws an error if in the released status state
     */
    public void confirm() {
        if (status == ReservationStatus.CONFIRMED) {
            return; // idempotent
        }
        if (status == ReservationStatus.RELEASED) {
            throw new IllegalStateException(
                    "Cannot confirm a released reservation"
            );
        }
        this.status = ReservationStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }



}
