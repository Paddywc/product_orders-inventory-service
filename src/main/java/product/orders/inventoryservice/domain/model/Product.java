package product.orders.inventoryservice.domain.model;

import jakarta.persistence.*;
import product.orders.inventoryservice.domain.exception.InsufficientInventoryException;

import java.util.UUID;

@Entity
@Table(name="product")
/**
 * The name and available quantity of a product that can be ordered
 */
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false, length = 255, name="product_name")
    private String name;


    @Column(nullable = false, name = "available_quantity")
    private int availableQuantity;

    @Version
    private Long version;


    public Product() {
    }

    public Product(UUID productId, String name, int availableQuantity) {
        this.productId = productId;
        this.name = name;
        this.availableQuantity = availableQuantity;
    }

    /* ---------- Getters and setters ---------- */

    public UUID getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    /* ---------- State transitions ---------- */

    /**
     * Remove the quantity from the available quantity. Throw an exception if not enough avialble to reserve
     * @param quantity the amount to reserve
     */
    public void reserve(int quantity){
        validatePositiveQuantity(quantity);
        if(quantity < availableQuantity){
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
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

}
