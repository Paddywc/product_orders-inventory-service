package product.orders.inventoryservice.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when a product is not found by its ID.
 */
public class NoProductFoundException extends RuntimeException {
    public NoProductFoundException(UUID productId) {
        super(
                String.format("No product found with ID: %s", productId)
        );
    }
}
