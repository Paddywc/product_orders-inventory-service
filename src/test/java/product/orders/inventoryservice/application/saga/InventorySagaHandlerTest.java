package product.orders.inventoryservice.application.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import product.orders.inventoryservice.application.InventoryApplicationService;
import product.orders.inventoryservice.domain.exception.InsufficientInventoryException;
import product.orders.inventoryservice.messaging.event.*;
import product.orders.inventoryservice.messaging.producer.InventoryEventProducer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


class InventorySagaHandlerTest {

    private InventoryApplicationService inventoryApplicationService;

    private InventoryEventProducer eventProducer;

    private InventoryReservationTxService inventoryReservationTxService;

    private InventorySagaHandler inventorySagaHandler;

    @BeforeEach
    void setUp() {
        inventoryApplicationService = mock(InventoryApplicationService.class);
        eventProducer = mock(InventoryEventProducer.class);
        inventoryReservationTxService = new InventoryReservationTxService(inventoryApplicationService);
        inventorySagaHandler = new InventorySagaHandler(inventoryApplicationService, eventProducer, inventoryReservationTxService);
    }


    @Test
    void testReserveInventory_SendValidItems_shouldReserveAllItemsAndPublishReservedEvent() {
        // Arrange
        InventorySagaHandler sagaHandler = new InventorySagaHandler(inventoryApplicationService, eventProducer, inventoryReservationTxService);

        UUID orderId = UUID.randomUUID();
        UUID product1 = UUID.randomUUID();
        UUID product2 = UUID.randomUUID();
        List<OrderItem> items = List.of(
                new OrderItem(product1, 5),
                new OrderItem(product2, 10)
        );
        OrderCreatedEvent event = OrderCreatedEvent.of(orderId, 1500L, "EUR", UUID.randomUUID(), "anemail@email.com", "123 Road", items);

        // Act
        sagaHandler.reserveInventory(event);

        // Assert
        for (OrderItem item : items) {
            verify(inventoryApplicationService).reserve(orderId, item.productId(), item.quantity());
        }
        verify(eventProducer).publish(any(InventoryReservedEvent.class));
    }

    @Test
    void testReserveInventory_WhenReservingMoreThanAvailableQuantity_PublishReservationFailedEvent() {
        // Arrange
        InventorySagaHandler sagaHandler = new InventorySagaHandler(inventoryApplicationService, eventProducer, inventoryReservationTxService);

        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int requestedQuantity = 5;
        int availableQuantity = 2;

        List<OrderItem> items = List.of(new OrderItem(productId, requestedQuantity));
        OrderCreatedEvent event = OrderCreatedEvent.of(orderId, 1500L, "EUR", UUID.randomUUID(), "anemail@email.com", "123 Road", items);

        doThrow(new InsufficientInventoryException(productId, requestedQuantity, availableQuantity))
                .when(inventoryApplicationService)
                .reserve(orderId, productId, requestedQuantity);

        when(inventoryApplicationService.getAvailableQuantity(productId)).thenReturn(availableQuantity);

        ArgumentCaptor<InventoryReservationFailedEvent> eventCaptor = ArgumentCaptor.forClass(InventoryReservationFailedEvent.class);

        // Act
        sagaHandler.reserveInventory(event);

        // Assert
        verify(eventProducer).publish(eventCaptor.capture());
        assertEquals(InventoryReservationFailedReason.INSUFFICIENT_INVENTORY, eventCaptor.getValue().reason());
    }

    @Test
    void testReserveInventory_InventoryServiceThrowsIllegalArgumentException_PublishReservationFailedEvent() {
        // Arrange
        InventorySagaHandler sagaHandler = new InventorySagaHandler(inventoryApplicationService, eventProducer, inventoryReservationTxService);

        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int requestedQuantity = 4;
        int availableQuantity = 10;

        List<OrderItem> items = List.of(new OrderItem(productId, requestedQuantity));
        OrderCreatedEvent event = OrderCreatedEvent.of(
                orderId,
                1500L,
                "EUR",
                UUID.randomUUID(),
                "email@e.com",
                "123 Road",
                items);

        doThrow(new IllegalArgumentException())
                .when(inventoryApplicationService)
                .reserve(orderId, productId, requestedQuantity);

        when(inventoryApplicationService.getAvailableQuantity(productId)).thenReturn(availableQuantity);

        ArgumentCaptor<InventoryReservationFailedEvent> eventCaptor = ArgumentCaptor.forClass(InventoryReservationFailedEvent.class);


        // Act
        sagaHandler.reserveInventory(event);

        // Assert
        verify(eventProducer).publish(eventCaptor.capture());
        assertEquals(InventoryReservationFailedReason.INVALID_REQUEST, eventCaptor.getValue().reason());
    }

    @Test
    void testReserveInventory_InventoryServiceThrowsDataIntegrityViolationException_PublishReservationFailedEvent() {
        // Arrange
        InventorySagaHandler sagaHandler = new InventorySagaHandler(inventoryApplicationService, eventProducer, inventoryReservationTxService);

        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int requestedQuantity = 4;
        int availableQuantity = 10;

        List<OrderItem> items = List.of(new OrderItem(productId, requestedQuantity));
        OrderCreatedEvent event = OrderCreatedEvent.of(orderId, 1500L, "EUR", UUID.randomUUID(), "anemail@email.com", "123 Road", items);

        doThrow(new DataIntegrityViolationException(""))
                .when(inventoryApplicationService)
                .reserve(orderId, productId, requestedQuantity);

        when(inventoryApplicationService.getAvailableQuantity(productId)).thenReturn(availableQuantity);

        ArgumentCaptor<InventoryReservationFailedEvent> eventCaptor = ArgumentCaptor.forClass(InventoryReservationFailedEvent.class);

        // Act
        sagaHandler.reserveInventory(event);

        // Assert
        verify(eventProducer).publish(eventCaptor.capture());
        assertEquals(InventoryReservationFailedReason.DUPLICATE_RESERVATION, eventCaptor.getValue().reason());
    }


    @Test
    void testReleaseInventory_PassedValidEvent_CallsInventoryServiceAndPublishesEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(),
                orderId,
                CancellationReason.SYSTEM_ERROR,
                Instant.now()
        );

        // Act
        inventorySagaHandler.releaseInventory(event);

        // Assert
        verify(inventoryApplicationService).release(orderId);
        verify(eventProducer).publish(any(InventoryReleasedEvent.class));
    }


    @Test
    void testReleaseInventory_ServiceThrowsIllegalStateException_DoesNothing() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(),
                orderId,
                CancellationReason.SYSTEM_ERROR,
                Instant.now()
        );
        doThrow(new IllegalStateException())
                .when(inventoryApplicationService)
                .release(orderId);

        // Act
        inventorySagaHandler.releaseInventory(event);

        // Assert
        verifyNoInteractions(eventProducer);
    }

    @Test
    void testReleaseInventory_ServiceThrowsIllegalArgumentException_DoesNothing() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(),
                orderId,
                CancellationReason.USER_CANCELLED,
                Instant.now()
        );
        doThrow(new IllegalArgumentException())
                .when(inventoryApplicationService)
                .release(orderId);

        // Act
        inventorySagaHandler.releaseInventory(event);

        // Assert
        verifyNoInteractions(eventProducer);
    }


    @Test
    void testConfirmInventory_PassedValidEvent_CallsInventoryServiceAndPublishesEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                UUID.randomUUID(),
                orderId,
                Instant.now()
        );

        // Act
        inventorySagaHandler.confirmInventory(event);

        // Assert
        verify(inventoryApplicationService).confirm(orderId);
        verify(eventProducer).publish(any(InventoryConfirmedEvent.class));
    }

    @Test
    void testConfirmInventory_ServiceThrowsIllegalStateException_PublishReservationFailedEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                UUID.randomUUID(),
                orderId,
                Instant.now()
        );

        doThrow(new IllegalStateException())
                .when(inventoryApplicationService)
                .confirm(orderId);
        ArgumentCaptor<InventoryReservationFailedEvent> eventCaptor = ArgumentCaptor.forClass(InventoryReservationFailedEvent.class);

        // Act
        inventorySagaHandler.confirmInventory(event);

        // Assert
        verify(eventProducer).publish(eventCaptor.capture());
        assertEquals(InventoryReservationFailedReason.ILLEGAL_RESERVATION_STATE, eventCaptor.getValue().reason());
    }


}