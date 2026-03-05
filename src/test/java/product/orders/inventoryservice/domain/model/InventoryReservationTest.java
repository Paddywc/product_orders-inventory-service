package product.orders.inventoryservice.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class InventoryReservationTest {


    @Test
    void testReserve_ValidInput_CreatesReservationInReservedState() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 5;

        // Act
        InventoryReservation reservation = InventoryReservation.reserve(orderId, productId, quantity);

        // Assert
        assertNotNull(reservation.getReservationId());
        assertEquals(orderId, reservation.getOrderId());
        assertEquals(productId, reservation.getProductId());
        assertEquals(quantity, reservation.getQuantity());
        assertEquals(ReservationStatus.RESERVED, reservation.getStatus());
        assertNotNull(reservation.getCreatedAt());
        assertNotNull(reservation.getUpdatedAt());
    }


    @Test
    void testReserve_NonPositiveQuantity_ThrowsException() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> InventoryReservation.reserve(orderId, productId, -1)
        );
        assertThat(exception.getMessage()).contains("must be positive");
    }

    @Test
    void testRelease_InReservedState_MovesToReleased(){
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 5;

        // Act
        InventoryReservation reservation = InventoryReservation.reserve(orderId, productId, quantity);
        reservation.release();

        // Assert
        assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
    }

    @Test
    void testRelease_InConfirmedState_ThrowsIllegalStateException(){
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 5;

        // Act
        InventoryReservation reservation = InventoryReservation.reserve(orderId, productId, quantity);
        reservation.confirm();

        // Assert
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertThatThrownBy(reservation::release)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("confirmed");
    }

    @Test
    void testRelease_calledMultipleTimes_InReleaseState(){
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 5;

        // Act
        InventoryReservation reservation = InventoryReservation.reserve(orderId, productId, quantity);
        reservation.release();
        reservation.release();
        reservation.release();
        reservation.release();

        // Assert
        assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
    }

    @Test
    void testConfirm_InReservedState_MovesToConfirmed(){
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 5;

        // Act
        InventoryReservation reservation = InventoryReservation.reserve(orderId, productId, quantity);
        reservation.confirm();

        // Assert
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }

    @Test
    void testConfirmed_InReleasedState_ThrowsIllegalStateException(){
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 5;

        // Act
        InventoryReservation reservation = InventoryReservation.reserve(orderId, productId, quantity);
        reservation.release();

        // Assert
        assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("released");
    }

    @Test
    void testConfirm_calledMultipleTimes_InConfirmedState(){
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 5;

        // Act
        InventoryReservation reservation = InventoryReservation.reserve(orderId, productId, quantity);
        reservation.confirm();
        reservation.confirm();

        // Assert
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }
}