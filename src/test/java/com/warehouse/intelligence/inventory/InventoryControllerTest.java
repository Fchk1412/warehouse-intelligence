package com.warehouse.intelligence.inventory;

import com.warehouse.intelligence.common.exception.BusinessRuleException;
import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.inventory.dto.CreateInventoryRequest;
import com.warehouse.intelligence.inventory.dto.InventoryDto;
import com.warehouse.intelligence.inventory.dto.UpdateInventoryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    private InventoryDto sampleInventoryDto(int quantity, int minStockLevel, boolean lowStock) {
        return new InventoryDto(
                100L,
                1L,
                "LAPTOP-001",
                "Lenovo ThinkPad",
                2L,
                "MOD-01",
                "Modena Central Warehouse",
                quantity,
                minStockLevel,
                lowStock,
                Instant.parse("2026-11-06T08:30:00Z")
        );
    }

    @Test
    void shouldReturnPagedInventories() throws Exception {
        // Arrange
        InventoryDto dto = sampleInventoryDto(25, 10, false);
        Page<InventoryDto> page = new PageImpl<>(List.of(dto));

        when(inventoryService.getAllInventories(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/inventories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].productSku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.content[0].warehouseCode").value("MOD-01"))
                .andExpect(jsonPath("$.content[0].quantity").value(25))
                .andExpect(jsonPath("$.content[0].lowStock").value(false));
    }

    @Test
    void shouldReturnFilteredInventoriesWhenQueryParamsProvided() throws Exception {
        // Arrange
        InventoryDto dto = sampleInventoryDto(25, 10, false);
        Page<InventoryDto> page = new PageImpl<>(List.of(dto));

        when(inventoryService.getAllInventories(eq(1L), eq(2L), any(Pageable.class)))
                .thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/inventories")
                        .param("productId", "1")
                        .param("warehouseId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productId").value(1))
                .andExpect(jsonPath("$.content[0].warehouseId").value(2));
    }

    @Test
    void shouldReturnInventoryByIdWhenExists() throws Exception {
        // Arrange
        InventoryDto dto = sampleInventoryDto(25, 10, false);
        when(inventoryService.getInventoryById(100L)).thenReturn(dto);

        // Act + Assert
        mockMvc.perform(get("/api/inventories/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.productSku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.warehouseCode").value("MOD-01"))
                .andExpect(jsonPath("$.quantity").value(25));
    }

    @Test
    void shouldReturnNotFoundWhenInventoryDoesNotExist() throws Exception {
        // Arrange
        when(inventoryService.getInventoryById(999L))
                .thenThrow(new ResourceNotFoundException("Inventory record not found"));

        // Act + Assert
        mockMvc.perform(get("/api/inventories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Inventory record not found"))
                .andExpect(jsonPath("$.path").value("/api/inventories/999"));
    }

    @Test
    void shouldReturnLowStockInventories() throws Exception {
        // Arrange
        InventoryDto lowStockDto = sampleInventoryDto(8, 10, true);
        Page<InventoryDto> page = new PageImpl<>(List.of(lowStockDto));

        when(inventoryService.getLowStockInventories(any(Pageable.class))).thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/inventories/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].quantity").value(8))
                .andExpect(jsonPath("$.content[0].minStockLevel").value(10))
                .andExpect(jsonPath("$.content[0].lowStock").value(true));
    }

    @Test
    void shouldCreateInventoryAndReturn201() throws Exception {
        // Arrange
        InventoryDto dto = sampleInventoryDto(30, 10, false);
        when(inventoryService.createInventory(any(CreateInventoryRequest.class))).thenReturn(dto);

        String requestBody = """
                {
                    "productId": 1,
                    "warehouseId": 2,
                    "quantity": 30,
                    "minStockLevel": 10
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.productSku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.warehouseCode").value("MOD-01"))
                .andExpect(jsonPath("$.quantity").value(30));
    }

    @Test
    void shouldReturn400WhenCreateInventoryRequestIsInvalid() throws Exception {
        // Arrange: missing productId and negative quantity
        String invalidBody = """
                {
                    "warehouseId": 2,
                    "quantity": -5,
                    "minStockLevel": 10
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.productId").exists())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());

        verify(inventoryService, never()).createInventory(any());
    }

    @Test
    void shouldReturn409WhenCreatingDuplicateInventory() throws Exception {
        // Arrange
        when(inventoryService.createInventory(any(CreateInventoryRequest.class)))
                .thenThrow(new DuplicateResourceException("Inventory record already exists for this product and warehouse"));

        String requestBody = """
                {
                    "productId": 1,
                    "warehouseId": 2,
                    "quantity": 30,
                    "minStockLevel": 10
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Inventory record already exists for this product and warehouse"));
    }

    @Test
    void shouldReturn409WhenCreatingInventoryForInactiveResource() throws Exception {
        // Arrange
        when(inventoryService.createInventory(any(CreateInventoryRequest.class)))
                .thenThrow(new BusinessRuleException("Cannot create inventory for an inactive product"));

        String requestBody = """
                {
                    "productId": 1,
                    "warehouseId": 2,
                    "quantity": 30,
                    "minStockLevel": 10
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot create inventory for an inactive product"));
    }

    @Test
    void shouldUpdateInventoryAndReturn200() throws Exception {
        // Arrange
        InventoryDto updatedDto = sampleInventoryDto(25, 15, false);
        when(inventoryService.updateInventory(eq(100L), any(UpdateInventoryRequest.class)))
                .thenReturn(updatedDto);

        String requestBody = """
                {
                    "minStockLevel": 15
                }
                """;

        // Act + Assert
        mockMvc.perform(put("/api/inventories/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(25))
                .andExpect(jsonPath("$.minStockLevel").value(15))
                .andExpect(jsonPath("$.lowStock").value(false));
    }

    @Test
    void shouldReturn400WhenUpdateInventoryRequestHasNegativeMinStock() throws Exception {
        // Arrange
        String invalidBody = """
                {
                    "minStockLevel": -1
                }
                """;

        // Act + Assert
        mockMvc.perform(put("/api/inventories/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.minStockLevel").exists());

        verify(inventoryService, never()).updateInventory(any(), any());
    }
}

