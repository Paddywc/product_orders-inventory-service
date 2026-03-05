package product.orders.inventoryservice.api.dto;

import java.util.UUID;

public record GetProductResponse(UUID productId, int availableQuantity) {
}
