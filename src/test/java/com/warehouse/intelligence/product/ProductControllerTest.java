package com.warehouse.intelligence.product;

import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.product.dto.CreateProductRequest;
import com.warehouse.intelligence.product.dto.ProductDto;
import com.warehouse.intelligence.product.dto.UpdateProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private ProductDto sampleProductDto() {
        return new ProductDto(
                "Lenovo ThinkPad",
                "LAPTOP-001",
                "Business laptop",
                new BigDecimal("1299.99")
        );
    }

    @Test
    void shouldReturnPagedProducts() throws Exception {
        // Arrange
        Page<ProductDto> page = new PageImpl<>(List.of(sampleProductDto()));
        when(productService.getAllProducts(any(Pageable.class))).thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.content[0].name").value("Lenovo ThinkPad"))
                .andExpect(jsonPath("$.content[0].unitPrice").value(1299.99));
    }

    @Test
    void shouldReturnProductByIdWhenExists() throws Exception {
        // Arrange
        when(productService.getProductById(1L)).thenReturn(sampleProductDto());

        // Act + Assert
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.name").value("Lenovo ThinkPad"));
    }

    @Test
    void shouldReturn404WhenProductDoesNotExist() throws Exception {
        // Arrange
        when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        // Act + Assert
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found"))
                .andExpect(jsonPath("$.path").value("/api/products/999"));
    }

    @Test
    void shouldCreateProductAndReturn201() throws Exception {
        // Arrange
        when(productService.createProduct(any(CreateProductRequest.class)))
                .thenReturn(sampleProductDto());

        String requestBody = """
                {
                    "sku": "LAPTOP-001",
                    "name": "Lenovo ThinkPad",
                    "description": "Business laptop",
                    "unitPrice": 1299.99
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.name").value("Lenovo ThinkPad"));
    }

    @Test
    void shouldReturn400WhenCreateProductRequestIsInvalid() throws Exception {
        // Arrange: blank sku and negative price
        String invalidBody = """
                {
                    "sku": "",
                    "name": "Lenovo ThinkPad",
                    "description": "Business laptop",
                    "unitPrice": -10.00
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.sku").exists())
                .andExpect(jsonPath("$.validationErrors.unitPrice").exists());

        verify(productService, never()).createProduct(any());
    }

    @Test
    void shouldReturn409WhenCreatingProductWithDuplicateSku() throws Exception {
        // Arrange
        when(productService.createProduct(any(CreateProductRequest.class)))
                .thenThrow(new DuplicateResourceException("SKU already exists"));

        String requestBody = """
                {
                    "sku": "LAPTOP-001",
                    "name": "Lenovo ThinkPad",
                    "description": "Business laptop",
                    "unitPrice": 1299.99
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("SKU already exists"));
    }

    @Test
    void shouldUpdateProductAndReturn200() throws Exception {
        // Arrange
        ProductDto updated = new ProductDto(
                "Lenovo ThinkPad T14",
                "LAPTOP-001",
                "Updated business laptop",
                new BigDecimal("1399.99")
        );
        when(productService.updateProduct(eq(1L), any(UpdateProductRequest.class)))
                .thenReturn(updated);

        String requestBody = """
                {
                    "sku": "LAPTOP-001",
                    "name": "Lenovo ThinkPad T14",
                    "description": "Updated business laptop",
                    "unitPrice": 1399.99
                }
                """;

        // Act + Assert
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lenovo ThinkPad T14"))
                .andExpect(jsonPath("$.unitPrice").value(1399.99));
    }

    @Test
    void shouldDeleteProductAndReturn204() throws Exception {
        // Arrange
        when(productService.deleteProduct(1L)).thenReturn(sampleProductDto());

        // Act + Assert
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }
}

