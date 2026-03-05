package product.orders.inventoryservice.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import product.orders.inventoryservice.domain.exception.NoProductFoundException;
import product.orders.inventoryservice.domain.model.InventoryReservation;
import product.orders.inventoryservice.domain.model.Product;
import product.orders.inventoryservice.domain.model.ReservationStatus;
import product.orders.inventoryservice.repository.InventoryReservationRepository;
import product.orders.inventoryservice.repository.ProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@DirtiesContext
class InventoryApplicationServiceImplTest {

    private ProductRepository productRepository;

    private InventoryReservationRepository inventoryReservationRepository;

    private InventoryApplicationServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        inventoryReservationRepository = mock(InventoryReservationRepository.class);
        inventoryService = new InventoryApplicationServiceImpl(productRepository, inventoryReservationRepository);
    }

    @Test
    void testReserve_PassedExistingProductWithoutReservation_savesInventoryReservation() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 5;

        Product product = mock(Product.class);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        // Return null to verify that no existing reservation exists
        when(inventoryReservationRepository.findByOrderIdAndProductId(orderId, productId))
                .thenReturn(null);

        // Act
        inventoryService.reserve(orderId, productId, quantity);

        // Assert
        ArgumentCaptor<InventoryReservation> reservationCaptor = ArgumentCaptor.forClass(InventoryReservation.class);
        verify(inventoryReservationRepository).save(reservationCaptor.capture());

        InventoryReservation reservation = reservationCaptor.getValue();
        assertEquals(orderId, reservation.getOrderId());
        assertEquals(productId, reservation.getProductId());
        assertEquals(quantity, reservation.getQuantity());
        assertEquals(ReservationStatus.RESERVED, reservation.getStatus());

        verify(productRepository).save(product);
    }

    @Test
    void testReserve_ReservesAndSavesProduct_WhenProductExist() {
        // Arrange
        Product product = mock(Product.class);
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 3;

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));
        when(inventoryReservationRepository
                .findByOrderIdAndProductId(orderId, productId))
                .thenReturn(null);

        // Act
        inventoryService.reserve(orderId, productId, quantity);

        // Assert
        verify(product).reserve(quantity);
        verify(productRepository).save(product);
    }

    @Test
    void testReserve_WhenNoProductFound_ThrowsException() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 5;
        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.reserve(orderId, productId, quantity))
                .isInstanceOf(NoProductFoundException.class)
                .hasMessageContaining("No product found");
        verifyNoInteractions(inventoryReservationRepository);
    }

    @Test
    void testReserve_WhenReservationExistsForProductAndOrder_ThrowsException() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 2;
        when(inventoryReservationRepository.findByOrderIdAndProductId(
                orderId,
                productId
        )).thenReturn(mock(InventoryReservation.class));
        Product product = mock(Product.class);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.reserve(orderId, productId, quantity))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("already");
        verify(productRepository, never()).save(any());
        verify(inventoryReservationRepository, never()).save(any());
    }


    @Test
    void testRelease_WhenGivenOrderWithReservations_ReleasesReservationProducts() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        Product reservationOneProduct = mock(Product.class);
        UUID productOneId = UUID.randomUUID();
        Product reservationTwoProduct = mock(Product.class);
        UUID productTwoId = UUID.randomUUID();


        InventoryReservation reservationOne = mock(InventoryReservation.class);
        InventoryReservation reservationTwo = mock(InventoryReservation.class);
        when(reservationOne.getStatus()).thenReturn(ReservationStatus.RESERVED);
        when(reservationTwo.getStatus()).thenReturn(ReservationStatus.RESERVED);
        when(reservationOne.getProductId()).thenReturn(productOneId);
        when(reservationTwo.getProductId()).thenReturn(productTwoId);
        int reservationOneQuantity = 5;
        int reservationTwoQuantity = 3;
        when(reservationOne.getQuantity()).thenReturn(reservationOneQuantity);
        when(reservationTwo.getQuantity()).thenReturn(reservationTwoQuantity);

        when(inventoryReservationRepository.findByOrderId(orderId))
                .thenReturn(List.of(reservationOne, reservationTwo));
        when(productRepository.findById(productOneId))
                .thenReturn(Optional.of(reservationOneProduct));
        when(productRepository.findById(productTwoId))
                .thenReturn(Optional.of(reservationTwoProduct));

        // Act
        inventoryService.release(orderId);

        // Assert
        verify(reservationOne).release();
        verify(reservationTwo).release();
        verify(reservationOneProduct).release(reservationOneQuantity);
        verify(reservationTwoProduct).release(reservationTwoQuantity);

        verify(productRepository).save(reservationOneProduct);
        verify(productRepository).save(reservationTwoProduct);
        verify(inventoryReservationRepository).save(reservationOne);
        verify(inventoryReservationRepository).save(reservationTwo);
    }

    @Test
    void testRelease_WhenGivenNoneReservedStatusReservations_SkipsReservation() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        InventoryReservation reservation = mock(InventoryReservation.class);

        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(inventoryReservationRepository.findByOrderId(orderId))
                .thenReturn(List.of(reservation));

        // Act
        inventoryService.release(orderId);


        // Assert
        verify(reservation, never()).release();
        verify(productRepository, never()).save(any());
        verify(inventoryReservationRepository, never()).save(any());
    }

    @Test
    void testConfirm_WhenGivenOrderWithReservations_ConfirmsReservationProducts() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        InventoryReservation reservationOne = mock(InventoryReservation.class);
        InventoryReservation reservationTwo = mock(InventoryReservation.class);
        when(reservationOne.getStatus()).thenReturn(ReservationStatus.RESERVED);
        when(reservationTwo.getStatus()).thenReturn(ReservationStatus.RESERVED);

        when(inventoryReservationRepository.findByOrderId(orderId))
                .thenReturn(List.of(reservationOne, reservationTwo));


        // Act
        inventoryService.confirm(orderId);

        // Assert
        verify(reservationOne).confirm();
        verify(reservationTwo).confirm();

        verify(inventoryReservationRepository).save(reservationOne);
        verify(inventoryReservationRepository).save(reservationTwo);
    }

    @Test
    void testGetAvailableQuantity_WhenGivenProduct_ReturnsAvailableQuantity() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Product product = mock(Product.class);
        int availableQuantity = 4;

        when(product.getAvailableQuantity()).thenReturn(availableQuantity);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act
        int result = inventoryService.getAvailableQuantity(productId);

        // Assert
        assertEquals(availableQuantity, result);
    }

    @Test
    void testGetAvailableQuantity_WhenProductNotFound_ReturnsZero() {
        // Arrange
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act
        int result = inventoryService.getAvailableQuantity(productId);

        // Assert
        assertEquals(0, result);
    }


}