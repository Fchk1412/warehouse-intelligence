package com.warehouse.intelligence.supplier;

import com.warehouse.intelligence.common.exception.BusinessRuleException;
import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.supplier.dto.CreateSupplierProductRequest;
import com.warehouse.intelligence.supplier.dto.SupplierProductDto;
import com.warehouse.intelligence.supplier.dto.UpdateSupplierProductRequest;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupplierProductController.class)
class SupplierProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierProductService supplierProductService;

    private SupplierProductDto sampleDto() {
        return new SupplierProductDto(
                10L,
                1L,
                "SUP-LENOVO",
                "Lenovo Italy S.r.l.",
                2L,
                "LAPTOP-001",
                "Lenovo ThinkPad T14",
                new BigDecimal("1100.00"),
                5,
                true
        );
    }

    @Test
    void shouldReturnPagedSupplierProducts() throws Exception {
        // Arrange
        Page<SupplierProductDto> page = new PageImpl<>(List.of(sampleDto()));
        when(supplierProductService.getAllSupplierProducts(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/supplier-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].supplierCode").value("SUP-LENOVO"))
                .andExpect(jsonPath("$.content[0].productSku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.content[0].supplyPrice").value(1100.00))
                .andExpect(jsonPath("$.content[0].leadTimeDays").value(5));
    }

    @Test
    void shouldReturnFilteredSupplierProductsWhenQueryParamsProvided() throws Exception {
        // Arrange
        Page<SupplierProductDto> page = new PageImpl<>(List.of(sampleDto()));
        when(supplierProductService.getAllSupplierProducts(eq(1L), eq(2L), any(Pageable.class)))
                .thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/supplier-products")
                        .param("supplierId", "1")
                        .param("productId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].supplierId").value(1))
                .andExpect(jsonPath("$.content[0].productId").value(2));
    }

    @Test
    void shouldReturnSupplierProductByIdWhenExists() throws Exception {
        // Arrange
        when(supplierProductService.getSupplierProductById(10L)).thenReturn(sampleDto());

        // Act + Assert
        mockMvc.perform(get("/api/supplier-products/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.supplierCode").value("SUP-LENOVO"))
                .andExpect(jsonPath("$.productSku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.supplyPrice").value(1100.00));
    }

    @Test
    void shouldReturn404WhenSupplierProductDoesNotExist() throws Exception {
        // Arrange
        when(supplierProductService.getSupplierProductById(999L))
                .thenThrow(new ResourceNotFoundException("Supplier product offering not found"));

        // Act + Assert
        mockMvc.perform(get("/api/supplier-products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Supplier product offering not found"));
    }

    @Test
    void shouldCreateSupplierProductAndReturn201() throws Exception {
        // Arrange
        when(supplierProductService.createSupplierProduct(any(CreateSupplierProductRequest.class)))
                .thenReturn(sampleDto());

        String requestBody = """
                {
                    "supplierId": 1,
                    "productId": 2,
                    "supplyPrice": 1100.00,
                    "leadTimeDays": 5
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/supplier-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.supplyPrice").value(1100.00))
                .andExpect(jsonPath("$.leadTimeDays").value(5));
    }

    @Test
    void shouldReturn400WhenCreateSupplierProductHasInvalidPriceOrLeadTime() throws Exception {
        // Arrange: negative supplyPrice and leadTimeDays = 0
        String invalidBody = """
                {
                    "supplierId": 1,
                    "productId": 2,
                    "supplyPrice": -50.00,
                    "leadTimeDays": 0
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/supplier-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.supplyPrice").exists())
                .andExpect(jsonPath("$.validationErrors.leadTimeDays").exists());

        verify(supplierProductService, never()).createSupplierProduct(any());
    }

    @Test
    void shouldReturn409WhenSupplierAlreadyOffersProduct() throws Exception {
        // Arrange
        when(supplierProductService.createSupplierProduct(any(CreateSupplierProductRequest.class)))
                .thenThrow(new DuplicateResourceException("Supplier already offers this product"));

        String requestBody = """
                {
                    "supplierId": 1,
                    "productId": 2,
                    "supplyPrice": 1100.00,
                    "leadTimeDays": 5
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/supplier-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Supplier already offers this product"));
    }

    @Test
    void shouldReturn409WhenSupplierOrProductIsInactive() throws Exception {
        // Arrange
        when(supplierProductService.createSupplierProduct(any(CreateSupplierProductRequest.class)))
                .thenThrow(new BusinessRuleException("Cannot register offering for an inactive supplier"));

        String requestBody = """
                {
                    "supplierId": 1,
                    "productId": 2,
                    "supplyPrice": 1100.00
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/supplier-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot register offering for an inactive supplier"));
    }

    @Test
    void shouldUpdateSupplierProductAndReturn200() throws Exception {
        // Arrange
        SupplierProductDto updated = new SupplierProductDto(
                10L,
                1L,
                "SUP-LENOVO",
                "Lenovo Italy S.r.l.",
                2L,
                "LAPTOP-001",
                "Lenovo ThinkPad T14",
                new BigDecimal("1050.00"),
                7,
                true
        );
        when(supplierProductService.updateSupplierProduct(eq(10L), any(UpdateSupplierProductRequest.class)))
                .thenReturn(updated);

        String requestBody = """
                {
                    "supplyPrice": 1050.00,
                    "leadTimeDays": 7
                }
                """;

        // Act + Assert
        mockMvc.perform(put("/api/supplier-products/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplyPrice").value(1050.00))
                .andExpect(jsonPath("$.leadTimeDays").value(7));
    }

    @Test
    void shouldDeleteSupplierProductAndReturn204() throws Exception {
        // Arrange
        when(supplierProductService.deleteSupplierProduct(10L)).thenReturn(sampleDto());

        // Act + Assert
        mockMvc.perform(delete("/api/supplier-products/10"))
                .andExpect(status().isNoContent());

        verify(supplierProductService).deleteSupplierProduct(10L);
    }
}

