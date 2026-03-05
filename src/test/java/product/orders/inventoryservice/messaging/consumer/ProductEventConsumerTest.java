package product.orders.inventoryservice.messaging.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import product.orders.inventoryservice.application.InventoryApplicationService;
import product.orders.inventoryservice.persistance.ProcessedProductEvent;
import product.orders.inventoryservice.repository.ProcessedProductEventRepository;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductEventConsumerTest {

    @InjectMocks
    private ProductEventConsumer productEventConsumer;

    @Mock
    private InventoryApplicationService inventoryApplicationService;

    @Mock
    private ProcessedProductEventRepository processedProductEventRepository;


    @Test
    void testHandleEvent_PassedUnprocessedProductCreatedEVent_AddProduct() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String rawJson = """
                {
                "eventId": "%s",
                "productId": "%s"
                }
                """.formatted(eventId, productId);
        when(processedProductEventRepository.saveAndFlush(any(ProcessedProductEvent.class))).thenReturn(new ProcessedProductEvent(eventId));

        // Act
        productEventConsumer.handleEvent(rawJson, "ProductCreatedEvent");

        // Assert
        verify(inventoryApplicationService).addProduct(productId, 0);
    }

    @Test
    void testHandleEvent_PassedProcessedEvent_DoesNothing(){
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String rawJson = """
                {
                "eventId": "%s",
                "productId": "%s"
                }
                """.formatted(eventId, productId);
        when(processedProductEventRepository.saveAndFlush(any(ProcessedProductEvent.class)))
                .thenThrow(DataIntegrityViolationException.class);

        // Act
        productEventConsumer.handleEvent(rawJson, "ProductCreatedEvent");

        // Assert
        verify(inventoryApplicationService, times(0)).addProduct(productId, 0);
    }

}
