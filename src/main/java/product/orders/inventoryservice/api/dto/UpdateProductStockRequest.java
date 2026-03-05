package product.orders.inventoryservice.api.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateProductStockRequest(@PositiveOrZero int newStock) {

}
