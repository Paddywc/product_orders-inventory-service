package product.orders.inventoryservice.messaging.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import product.orders.inventoryservice.application.saga.InventorySagaHandler;
import product.orders.inventoryservice.messaging.event.*;
import product.orders.inventoryservice.persistance.ProcessedOrderEvent;
import product.orders.inventoryservice.repository.ProcessedOrderEventRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    @Mock
    private InventorySagaHandler sagaHandler;

    @Mock
    private ProcessedOrderEventRepository processedOrderEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    void testHandle_GivenOrderCreatedEvents_ReservesInventory() {
        // Arrange
        OrderCreatedEvent event = OrderCreatedEvent.of(
                UUID.randomUUID(),
                50L,
                "EUR",
                UUID.randomUUID(),
                "fake@email.com",
                "Dublin, Ireland",
                List.of(new OrderItem(UUID.randomUUID(), 1))
        );

        // Act
        String eventAsString = objectMapper.writeValueAsString(event);
        orderEventConsumer.handleEvent(
                eventAsString,
                "OrderCreatedEvent");

        // Assert
        verify(sagaHandler).reserveInventory(event);
    }

    @Test
    void testHandle_GivenOrderCancelledEvent_ReleasesInventory() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        OrderCancelledEvent event = new OrderCancelledEvent(
                eventId,
                UUID.randomUUID(),
                CancellationReason.PAYMENT_FAILED,
                Instant.now()
        );

        // Act
        String eventAsString = objectMapper.writeValueAsString(event);
        orderEventConsumer.handleEvent(
                eventAsString,
                "OrderCancelledEvent"
        );

        // Assert
        verify(sagaHandler).releaseInventory(event);
    }

    @Test
    void testHandle_GivenOrderConfirmedEvent_CallSagaConfirmInventory() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                eventId,
                UUID.randomUUID(),
                Instant.now()
        );

        // Act
        String eventAsString = objectMapper.writeValueAsString(event);
        orderEventConsumer.handleEvent(
                eventAsString,
                "OrderConfirmedEvent");

        // Assert
        verify(sagaHandler).confirmInventory(event);
    }

    @Test
    void testHandle_EventIdFoundInRepository_DoesNothing() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                eventId,
                UUID.randomUUID(),
                Instant.now()
        );
        when(processedOrderEventRepository.saveAndFlush(any(ProcessedOrderEvent.class))).thenThrow(
                DataIntegrityViolationException.class
        );

        // Act
        String eventAsString = objectMapper.writeValueAsString(event);
        orderEventConsumer.handleEvent(
                eventAsString,
                "OrderConfirmedEvent");

        // Assert
        verifyNoInteractions(sagaHandler);
    }

}