package product.orders.inventoryservice.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import product.orders.inventoryservice.domain.model.InventoryReservation;
import product.orders.inventoryservice.domain.model.Product;
import product.orders.inventoryservice.domain.model.ReservationStatus;
import product.orders.inventoryservice.repository.InventoryReservationRepository;
import product.orders.inventoryservice.repository.ProductRepository;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class InventoryApplicationServiceIT {

    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("inv_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }


    @Autowired
    private InventoryApplicationService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Test
    void testReserve_PassedValidOrderAndProductIds_PersistsReservation() {
        // Arrange
        UUID productId = UUID.randomUUID();

        productRepository.save(new Product(productId, 10));
        UUID orderId = UUID.randomUUID();

        // Act
        inventoryService.reserve(orderId, productId, 5);

        // Assert
        Product reservedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(reservedProduct.getAvailableQuantity()).isEqualTo(5);

        InventoryReservation inventoryReservation = inventoryReservationRepository.findByOrderIdAndProductId(orderId, productId);
        assertThat(inventoryReservation).isNotNull();
        assertThat(inventoryReservation.getQuantity()).isEqualTo(5);
    }

    @Test
    void testConfirm_ProductIsReserved_PersistsAsConfirmed() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        productRepository.save(new Product(productId, 10));
        InventoryReservation reservation = InventoryReservation.reserve(orderId, productId, 5);
        inventoryReservationRepository.save(reservation);

        // Act
        inventoryService.confirm(orderId);

        // Assert
        InventoryReservation confirmedReservation = inventoryReservationRepository.findByOrderIdAndProductId(orderId, productId);
        assertThat(confirmedReservation).isNotNull();
        assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void testRelease_ProductIsReserved_PersistsAsReleased() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        productRepository.save(new Product(productId, 10));
        InventoryReservation reservation = InventoryReservation.reserve(orderId, productId, 5);
        inventoryReservationRepository.save(reservation);

        // Act
        inventoryService.release(orderId);

        // Assert
        InventoryReservation confirmedReservation = inventoryReservationRepository.findByOrderIdAndProductId(orderId, productId);
        assertThat(confirmedReservation).isNotNull();
        assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void testAddProduct_PassedPositiveQuantity_PersistsProduct(){
        // Arrange
        UUID productId = UUID.randomUUID();
        int quantity = 10;

        // Act
        inventoryService.addProduct(productId, quantity);

        // Assert
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getAvailableQuantity()).isEqualTo(quantity);
    }

    @Test
    void testUpdateStock_PassedPositiveQuantity_PersistsStock(){
        // Arrange
        UUID productId = UUID.randomUUID();
        int quantity = 10;
        Product product = new Product(productId, 5);
        productRepository.save(product);

        // Act
        inventoryService.updateStock(productId, quantity);

        // Assert
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getAvailableQuantity()).isEqualTo(quantity);
    }
}
