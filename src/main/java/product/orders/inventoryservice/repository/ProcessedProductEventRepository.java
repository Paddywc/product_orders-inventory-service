package product.orders.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import product.orders.inventoryservice.persistance.ProcessedProductEvent;

@Repository
public interface ProcessedProductEventRepository extends JpaRepository<ProcessedProductEvent, Long> {
}
