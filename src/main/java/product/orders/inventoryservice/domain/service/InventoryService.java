package product.orders.inventoryservice.domain.service;

import java.util.UUID;

public interface InventoryService {
    /**
     * Mark a product as reserved and update
     *
     * @param orderId   id of the order that the product is being reserved as part of
     * @param productId the id of the product being reserved
     * @param quantity  the amount being ordered
     */
    void reserve(UUID orderId, UUID productId, int quantity);

    /**
     * Release all product reservations for an order
     *
     * @param orderId the id of the order to release the reservations for
     */
    void release(UUID orderId);

    /**
     * Confirm all reserved products for an order
     *
     * @param orderId the id of the order to confirm the reservations for
     */
    void confirm(UUID orderId);


    /***
     * Get the number of available units of a product
     * @param productId the product to get the number of available units for
     * @return the number of available units of the product
     */
    int getAvailableQuantity(UUID productId);
}
