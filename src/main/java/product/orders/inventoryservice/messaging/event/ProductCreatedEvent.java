package product.orders.inventoryservice.messaging.event;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProductCreatedEvent(@NotNull UUID eventId,
                                  @NotNull UUID productId) {


}
