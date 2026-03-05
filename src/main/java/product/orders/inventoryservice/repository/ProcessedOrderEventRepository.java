package product.orders.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import product.orders.inventoryservice.persistance.ProcessedOrderEvent;

@Repository
public interface ProcessedOrderEventRepository extends JpaRepository<ProcessedOrderEvent, Long> {
}
