package product.orders.inventoryservice.domain.exception;

import java.util.UUID;

public class InsufficientInventoryException extends RuntimeException {


    public InsufficientInventoryException(UUID productId,
                                          int requestedQuantity,
                                          int availableQuantity) {
        super(buildMessage(productId, requestedQuantity, availableQuantity));
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
}
