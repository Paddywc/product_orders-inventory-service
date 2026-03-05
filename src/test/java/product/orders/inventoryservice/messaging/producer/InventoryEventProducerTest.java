package product.orders.inventoryservice.messaging.producer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import product.orders.inventoryservice.config.KafkaTopicsProperties;
import product.orders.inventoryservice.messaging.event.InventoryReservedEvent;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private KafkaTopicsProperties topics;

    @InjectMocks
    private InventoryEventProducer producer;

    @Test
    void testPublish_GivenEvent_SendsEventToInventoryEventsTopic() {
        // Arrange
        final UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event =
                new InventoryReservedEvent(
                        UUID.randomUUID(),
                        orderId,
                        Instant.now()
                );
        String topicName = "DummyTopicName";
        when(topics.getInventoryEvents()).thenReturn(topicName);
        ArgumentCaptor<Message<InventoryReservedEvent>> messageCaptor = ArgumentCaptor.forClass(Message.class);

        // Act
        producer.publish(event);

        // Assert
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<InventoryReservedEvent> capturedMessage = messageCaptor.getValue();
        assertEquals(topicName, capturedMessage.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals(orderId.toString(), capturedMessage.getHeaders().get(KafkaHeaders.KEY));
        assertEquals("InventoryReservedEvent", capturedMessage.getHeaders().get("eventType"));
    }

}