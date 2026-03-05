package product.orders.inventoryservice.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.orders.inventoryservice.domain.exception.NoProductFoundException;
import product.orders.inventoryservice.domain.model.InventoryReservation;
import product.orders.inventoryservice.domain.model.Product;
import product.orders.inventoryservice.domain.model.ReservationStatus;
import product.orders.inventoryservice.repository.InventoryReservationRepository;
import product.orders.inventoryservice.repository.ProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryApplicationServiceImpl implements InventoryApplicationService {
    private final ProductRepository productRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    public InventoryApplicationServiceImpl(ProductRepository productRepository, InventoryReservationRepository inventoryReservationRepository) {
        this.productRepository = productRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
    }

    /**
     * Mark a product as reserved and update
     *
     * @param orderId   id of the order that the product is being reserved as part of
     * @param productId the id of the product being reserved
     * @param quantity  the amount being ordered
     */
    @Override
    public void reserve(UUID orderId, UUID productId, int quantity) {
        // Get the product
        Optional<Product> result = productRepository.findById(productId);
        if (result.isEmpty()) {
            throw new NoProductFoundException(productId);
        }
        Product product = result.get();

        // Reserve the product
        product.reserve(quantity);

        // Check if item already has an inventory reservation
        InventoryReservation existingReservation = inventoryReservationRepository.findByOrderIdAndProductId(
                orderId,
                productId);
        if (existingReservation != null) {
            throw new DataIntegrityViolationException("Product with ID " + productId + " already has an inventory reservation for order " + orderId);
        }

        // Create the inventory reservation
        InventoryReservation inventoryReservation = InventoryReservation.reserve(orderId, productId, quantity);

        // Save the product and inventory reservation
        productRepository.save(product);
        inventoryReservationRepository.save(inventoryReservation);
    }

    /**
     * Either confirm or release all product reservations for an order
     *
     * @param orderId the id of the order to confirm or release the reservations for
     * @param release if true, release reservations, otherwise confirm
     */
    protected void confirmOrRelease(UUID orderId, boolean release) {
        Logger logger = LoggerFactory.getLogger(getClass());

        List<InventoryReservation> reservations =
                inventoryReservationRepository.findByOrderId(orderId);
        for (InventoryReservation reservation : reservations) {
            // If not in the expected status, skip
            if (reservation.getStatus() != ReservationStatus.RESERVED) {
                logger.debug("Reservation {} is not in the reserved state, skipping", reservation.getReservationId());
                continue;
            }

            // Release/comfirm the reservation
            if (release) {
                reservation.release();
            } else {
                reservation.confirm();
            }

            // Release the product if required
            if (release) {
                Product product = productRepository.findById(reservation.getProductId()).orElseThrow();
                product.release(reservation.getQuantity());

                productRepository.save(product);
            }

            // Save the reservation
            inventoryReservationRepository.save(reservation);
        }
    }

    /**
     * Release all product reservations for an order
     *
     * @param orderId the id of the order to release the reservations for
     */
    @Override
    @Transactional(value = "transactionManager")
    public void release(UUID orderId) {
        confirmOrRelease(orderId, true);
    }


    /**
     * Confirm all reserved products for an order
     *
     * @param orderId the id of the order to confirm the reservations for
     */
    @Override
    @Transactional(value = "transactionManager")
    public void confirm(UUID orderId) {
        confirmOrRelease(orderId, false);
    }

    @Override
    public int getAvailableQuantity(UUID productId) {
        Optional<Product> result = productRepository.findById(productId);

        // If no product, available quantity is 0
        return result.map(Product::getAvailableQuantity).orElse(0);

    }

    /**
     * Save a new product to the database.
     *
     * @param productId the product id
     * @param quantity  the quantity of the product. If null, defaults to 0
     */
    @Override
    public void addProduct(UUID productId, Integer quantity) {
        if (quantity == null) {
            quantity = 0;
        } else if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Product product = new Product(productId, quantity);
        productRepository.save(product);
    }


    /**
     * Set the new stock amount for a product.
     *
     * @param productId the product id
     * @param newStock  the new stock amount
     */
    @Override
    public void updateStock(UUID productId, int newStock) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.setAvailableQuantity(newStock);
        productRepository.save(product);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(UUID productId) {
        return productRepository.findById(productId).orElseThrow();
    }

}
