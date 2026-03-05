package product.orders.inventoryservice.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import product.orders.inventoryservice.api.dto.GetProductResponse;
import product.orders.inventoryservice.api.dto.UpdateProductStockRequest;
import product.orders.inventoryservice.application.InventoryApplicationService;
import product.orders.inventoryservice.domain.model.Product;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private InventoryApplicationService inventoryApplicationService;

    public InventoryController(InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    @GetMapping("/products")
    public Iterable<GetProductResponse> getAllProducts() {
        // Map from model product to get product response
        return inventoryApplicationService.getAllProducts().stream()
                .map(product -> new GetProductResponse(product.getId(), product.getAvailableQuantity()))
                .toList();
    }

    @GetMapping("/products/{productId}")
    public GetProductResponse getProductById(@PathVariable UUID productId) {
        // Verify that the product exists
        Product product = inventoryApplicationService.getProductById(productId);
        return new GetProductResponse(product.getProductId(), product.getAvailableQuantity());
    }

    @PatchMapping("/products/{productId}/stock")
    public ResponseEntity<Void> updateStock(@PathVariable UUID productId, @RequestBody @Valid UpdateProductStockRequest request) {
        inventoryApplicationService.updateStock(productId, request.newStock());
        return ResponseEntity.noContent().build();
    }


}
