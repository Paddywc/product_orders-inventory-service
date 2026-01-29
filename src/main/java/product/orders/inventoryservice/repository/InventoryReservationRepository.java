package product.orders.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import product.orders.inventoryservice.domain.model.InventoryReservation;

import java.util.List;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {

    /**
     * Find an inventory reservation by order id and product id
     *
     * @param orderId   id of the order
     * @param productId id of the product
     * @return the inventory reservation
     */
    InventoryReservation findByOrderIdAndProductId(String orderId, String productId);

    /**
     * Find an inventory reservation by order id
     *
     * @param orderId the id of the order
     * @return the inventory reservations
     */
    List<InventoryReservation> findByOrderID(String orderId);
}
