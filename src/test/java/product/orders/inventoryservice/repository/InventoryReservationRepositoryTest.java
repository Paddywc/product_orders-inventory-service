package product.orders.inventoryservice.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import product.orders.inventoryservice.domain.model.InventoryReservation;
import product.orders.inventoryservice.domain.model.ReservationStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class InventoryReservationRepositoryTest {

    @Autowired
    private InventoryReservationRepository repository;
    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Test
    void testFindByOrderIdAndProductId_ReservationExists_ReturnsReservation() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InventoryReservation reservation =
                InventoryReservation.reserve(orderId, productId, 5);

        repository.saveAndFlush(reservation);

        // Act
        InventoryReservation found =
                repository.findByOrderIdAndProductId(orderId, productId);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getOrderId()).isEqualTo(orderId);
        assertThat(found.getProductId()).isEqualTo(productId);
        assertThat(found.getQuantity()).isEqualTo(5);
        assertThat(found.getStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void testFindByOrderId_ReservationsExist_ReturnsAllReservations() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        InventoryReservation r1 = InventoryReservation.reserve(orderId, UUID.randomUUID(), 2);
        InventoryReservation r2 = InventoryReservation.reserve(orderId, UUID.randomUUID(), 3);

        repository.save(r1);
        repository.save(r2);
        repository.flush();

        // Act
        List<InventoryReservation> reservations =
                repository.findByOrderId(orderId);

        // Assert
        assertThat(reservations).hasSize(2);
    }

    @Test
    void testSave_DuplicateOrderAndProductId_ThrowsException() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        InventoryReservation reservationOne = InventoryReservation.reserve(orderId, productId, 5);
        InventoryReservation reservationTwo = InventoryReservation.reserve(orderId, productId, 10);

        repository.saveAndFlush(reservationOne);

        // Act & Assert
        assertThatThrownBy(() -> repository.saveAndFlush(reservationTwo)).isInstanceOf(DataIntegrityViolationException.class);
    }


}
