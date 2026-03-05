package product.orders.inventoryservice.domain.model;

import org.junit.jupiter.api.Test;
import product.orders.inventoryservice.domain.exception.InsufficientInventoryException;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void testReserve_WhenGivenPositiveQuantity_RemovesFromAvailableQuantity() {
        // Arrange
        Product product = new Product(UUID.randomUUID(),  10);

        // Act
        product.reserve(2);

        // Assert
        assertEquals(8, product.getAvailableQuantity());
    }

    /**
     * Tests for the reserve method in the Product class.
     * The reserve method removes a specified quantity from the product's stock
     * if sufficient stock is available and the quantity is positive. If there is
     * insufficient stock or the quantity is invalid, it throws the appropriate exception.
     */


    @Test
    void testReserve_ReserveMoreThanAvailableQuantity_ThrowsInsufficientInventoryException() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Product product = new Product(productId,  5);

        int reserveQuantity = 10;

        // Act and Assert
        InsufficientInventoryException exception = assertThrows(
                InsufficientInventoryException.class,
                () -> product.reserve(reserveQuantity)
        );

        assertTrue(exception.getMessage().contains(productId.toString()));
    }

    @Test
    void testReserve_WhenPassedZeroOrLess_ThrowsIllegalArgumentException(){
        // Arrange
        UUID productId = UUID.randomUUID();
        Product product = new Product(productId,  10);

        int zeroQuantity = 0;

        // Act and Assert
        assertThatThrownBy(() -> product.reserve(zeroQuantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zero");

        int negativeQuantity = -2;
        assertThatThrownBy(() -> product.reserve(negativeQuantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zero");
    }


    @Test
    void testRelease_GivenPositiveQuantity_IncreasesAvailableQuantity(){
        // Arrange
        Product product = new Product(UUID.randomUUID(),  3);

        // Act
        product.release(4);

        // Assert
        assertEquals(7, product.getAvailableQuantity());
    }

    @Test
    void testRelease_WhenPassedZeroOrLess_ThrowsIllegalArgumentException(){
        // Arrange
        UUID productId = UUID.randomUUID();
        Product product = new Product(productId,  10);

        int zeroQuantity = 0;

        // Act and Assert
        assertThatThrownBy(() -> product.release(zeroQuantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zero");

        int negativeQuantity = -2;
        assertThatThrownBy(() -> product.release(negativeQuantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zero");
    }
}