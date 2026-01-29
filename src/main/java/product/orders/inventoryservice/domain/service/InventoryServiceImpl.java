package product.orders.inventoryservice.domain.service;

import org.springframework.stereotype.Service;
import product.orders.inventoryservice.domain.model.InventoryReservation;
import product.orders.inventoryservice.domain.model.Product;
import product.orders.inventoryservice.domain.model.ReservationStatus;
import product.orders.inventoryservice.repository.InventoryReservationRepository;
import product.orders.inventoryservice.repository.ProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final ProductRepository productRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    public InventoryServiceImpl(ProductRepository productRepository, InventoryReservationRepository inventoryReservationRepository) {
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
        Optional<Product> result = productRepository.findById(productId.toString());
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Product with id " + productId + " not found");
        }
        Product product = result.get();

        // Reserve the product
        product.reserve(quantity);

        // Check if item already has an inventory reservation
        InventoryReservation inventoryReservation = inventoryReservationRepository.findByOrderIdAndProductId(
                orderId.toString(),
                productId.toString());
        if (inventoryReservation != null) {
            throw new IllegalArgumentException("Product with ID " + productId + " already has an inventory reservation for order " + orderId);
        }

        // Create the inventory reservation
        inventoryReservation = InventoryReservation.reserve(orderId, productId, quantity);

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
    private void confirmOrRelease(UUID orderId, boolean release) {
        List<InventoryReservation> reservations =
                inventoryReservationRepository.findByOrderID(orderId.toString());
        for (InventoryReservation reservation : reservations) {
            // If not in the expected status, skip
            if (reservation.getStatus() != ReservationStatus.RESERVED) {
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
                Product product = productRepository.findById(reservation.getProductId().toString()).orElseThrow();
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
    public void release(UUID orderId) {
        confirmOrRelease(orderId, true);
    }


    /**
     * Confirm all reserved products for an order
     *
     * @param orderId the id of the order to confirm the reservations for
     */
    @Override
    public void confirm(UUID orderId) {
        confirmOrRelease(orderId, false);
    }

    @Override
    public int getAvailableQuantity(UUID productId) {
        Optional<Product> result = productRepository.findById(productId.toString());

        // If no product, available quantity is 0
        if(result.isEmpty()){
            return 0;
        }

        return result.get().getAvailableQuantity();
    }
}
