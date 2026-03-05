package product.orders.inventoryservice.domain.model;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;
import product.orders.inventoryservice.domain.exception.InsufficientInventoryException;

import java.util.UUID;

/**
 * The name and available quantity of a product that can be ordered
 */
@Entity
@Table(name="product")
public class Product implements Persistable<UUID> {

    @Id
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;



    @Column(nullable = false, name = "available_quantity")
    private int availableQuantity;

    @Version
    private Long version;


    public Product() {
    }

    public Product(UUID productId, int availableQuantity) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
    }


    /* ---------- State transitions ---------- */

    /**
     * Remove the quantity from the available quantity. Throw an exception if not enough available to reserve
     * @param quantity the amount to reserve
     */
    public void reserve(int quantity){
        validatePositiveQuantity(quantity);
        if(quantity > availableQuantity){
            throw new InsufficientInventoryException(
                    productId,
                    quantity,
                    availableQuantity
            );
        }
        availableQuantity -= quantity;
    }


    /**
     * Add the parameter quantity to the available quantity
     * @param quantity the number to release
     */
    public void release(int quantity) {
        validatePositiveQuantity(quantity);
        this.availableQuantity += quantity;
    }

    /**
     * Throw an illegal argument exception if the quantity is not greater than 0
     * @param quantity the quantity to validate
     */
    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero. " + quantity + " given for product " + productId);
        }
    }


    /**
     * Forces spring to call persist instead of merge when saving the product first when id is set
     * but the version is null
     * @return is version null
     */
    @Override
    public boolean isNew() {
        return version == null;
    }



    /* ---------- Getters and setters ---------- */

    @Override
    public @Nullable UUID getId() {
        return productId;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}
