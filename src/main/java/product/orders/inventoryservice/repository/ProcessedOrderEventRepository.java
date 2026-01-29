package product.orders.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import product.orders.inventoryservice.persistance.ProcessedOrderEvent;

import java.util.UUID;

public interface ProcessedOrderEventRepository extends JpaRepository<ProcessedOrderEvent, UUID> {
}
