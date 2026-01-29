package product.orders.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import product.orders.inventoryservice.domain.model.Product;

public interface ProductRepository extends JpaRepository<Product, String> {
}
