package product.orders.inventoryservice.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import product.orders.inventoryservice.application.InventoryApplicationService;
import product.orders.inventoryservice.domain.model.Product;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers =InventoryController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet
                        .OAuth2ResourceServerAutoConfiguration.class
        })
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryApplicationService inventoryApplicationService;

    @Test
    void testGetAllProducts_ProductsExist_ReturnsGetProductResponseList() throws Exception {
        // Arrange
        UUID productOneId = UUID.randomUUID();
        UUID productTwoId = UUID.randomUUID();
        int productOneQuantity = 10;
        int productTwoQuantity = 5;

        Product productOne = mock(Product.class);
        Product productTwo = mock(Product.class);
        when(productOne.getId()).thenReturn(productOneId);
        when(productTwo.getId()).thenReturn(productTwoId);
        when(productOne.getAvailableQuantity()).thenReturn(productOneQuantity);
        when(productTwo.getAvailableQuantity()).thenReturn(productTwoQuantity);

        when(inventoryApplicationService.getAllProducts()).thenReturn(List.of(productOne, productTwo));

        // Act
        mockMvc.perform(get("/api/inventory/products"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(productOneId.toString()))
                .andExpect(jsonPath("$[0].availableQuantity").value(productOneQuantity))
                .andExpect(jsonPath("$[1].productId").value(productTwoId.toString()))
                .andExpect(jsonPath("$[1].availableQuantity").value(productTwoQuantity));
    }

    @Test
    void testGetProductById_HasValidQuantity_ReturnsProductWithQuantity() throws Exception {
        // Arrange
        UUID productId = UUID.randomUUID();
        int quantity = 10;
        Product product = mock(Product.class);
        when(product.getProductId()).thenReturn(productId);
        when(product.getAvailableQuantity()).thenReturn(quantity);
        when(inventoryApplicationService.getProductById(productId)).thenReturn(product);

        // Act
        mockMvc.perform(get("/api/inventory/products/{productId}", productId))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.availableQuantity").value(quantity));
    }

    @Test
    void testUpdateStock_ValidRequest_CallsApplicationServiceUpdateStock() throws Exception {
        // Arrange
        UUID productId = UUID.randomUUID();
        int quantity = 10;

        // Act
        String requestContent = """
                {
                    "newStock": %s
                }
                """.formatted(quantity);

        mockMvc.perform(patch("/api/inventory/products/{productId}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent))
                // Assert
                .andExpect(status().isNoContent());

        verify(inventoryApplicationService).updateStock(productId, quantity);
    }

    @Test
    void testUpdateStock_NegativeQuantity_ReturnBadRequest() throws Exception {
        // Arrange
        UUID productId = UUID.randomUUID();
        int quantity = -10;

        // Act
        String requestContent = """
                {
                    "newStock": %s
                }
                """.formatted(quantity);

        mockMvc.perform(patch("/api/inventory/products/{productId}/stock", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent))
                // Assert
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryApplicationService);
    }

}
