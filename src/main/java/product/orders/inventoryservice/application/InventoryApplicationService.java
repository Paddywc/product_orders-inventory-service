package product.orders.inventoryservice.application;

import product.orders.inventoryservice.domain.model.Product;

import java.util.List;
import java.util.UUID;

public interface InventoryApplicationService {
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

    /**
     * Save a new product to the database.
     * @param productId the product id
     * @param quantity the quantity of the product. If null, defaults to 0
     */
    void addProduct(UUID productId, Integer quantity);

    /**
     * Set the new stock amount for a product.
     *
     * @param productId the product id
     * @param newStock  the new stock amount
     */
    void updateStock(UUID productId, int newStock);

    List<Product> getAllProducts();

    Product getProductById(UUID productId);
}
