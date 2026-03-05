package product.orders.inventoryservice.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when an order cannot be fulfilled due to insufficient inventory.
 */
public class InsufficientInventoryException extends RuntimeException {

    private final UUID productId;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientInventoryException(UUID productId,
                                          int requestedQuantity,
                                          int availableQuantity) {
        super(buildMessage(productId, requestedQuantity, availableQuantity));
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }


    /**
     * Builds a formatted error message describing the inventory shortfall.
     *
     * @param productId         the unique identifier of the product
     * @param requestedQuantity the quantity that was requested
     * @param availableQuantity the quantity that is currently available
     * @return a formatted string message containing the product ID and inventory details
     */
    private static String buildMessage(
            UUID productId,
            int requestedQuantity,
            int availableQuantity
    ) {
        return String.format(
                "Insufficient inventory for product %s: requested=%d, available=%d",
                productId, requestedQuantity, availableQuantity
        );
    }

    // GETTERS
    public UUID getProductId() {
        return productId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
