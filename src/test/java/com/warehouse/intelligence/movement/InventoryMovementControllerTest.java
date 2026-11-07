package com.warehouse.intelligence.movement;

import com.warehouse.intelligence.common.exception.BusinessRuleException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.movement.dto.CreateInventoryMovementRequest;
import com.warehouse.intelligence.movement.dto.InventoryMovementDto;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryMovementController.class)
class InventoryMovementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryMovementService movementService;

    private InventoryMovementDto sampleMovementDto(MovementType type, int quantity, String reason) {
        return new InventoryMovementDto(
                500L,
                100L,
                1L,
                "LAPTOP-001",
                "Lenovo ThinkPad",
                2L,
                "MOD-01",
                "Modena Central Warehouse",
                type,
                quantity,
                reason,
                Instant.parse("2026-11-07T10:00:00Z")
        );
    }

    @Test
    void shouldReturnPagedMovements() throws Exception {
        // Arrange
        InventoryMovementDto dto = sampleMovementDto(MovementType.IN, 15, "Supplier delivery");
        Page<InventoryMovementDto> page = new PageImpl<>(List.of(dto));

        when(movementService.getAllMovements(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/inventory-movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(500))
                .andExpect(jsonPath("$.content[0].inventoryId").value(100))
                .andExpect(jsonPath("$.content[0].productSku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.content[0].movementType").value("IN"))
                .andExpect(jsonPath("$.content[0].quantity").value(15));
    }

    @Test
    void shouldReturnFilteredMovementsWhenQueryParamsProvided() throws Exception {
        // Arrange
        InventoryMovementDto dto = sampleMovementDto(MovementType.OUT, 5, "Customer shipment");
        Page<InventoryMovementDto> page = new PageImpl<>(List.of(dto));

        when(movementService.getAllMovements(eq(100L), eq(MovementType.OUT), any(Pageable.class)))
                .thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/inventory-movements")
                        .param("inventoryId", "100")
                        .param("movementType", "OUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].inventoryId").value(100))
                .andExpect(jsonPath("$.content[0].movementType").value("OUT"));
    }

    @Test
    void shouldReturnMovementByIdWhenExists() throws Exception {
        // Arrange
        InventoryMovementDto dto = sampleMovementDto(MovementType.IN, 15, "Supplier delivery");
        when(movementService.getMovementById(500L)).thenReturn(dto);

        // Act + Assert
        mockMvc.perform(get("/api/inventory-movements/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(500))
                .andExpect(jsonPath("$.inventoryId").value(100))
                .andExpect(jsonPath("$.movementType").value("IN"))
                .andExpect(jsonPath("$.quantity").value(15))
                .andExpect(jsonPath("$.reason").value("Supplier delivery"));
    }

    @Test
    void shouldReturn404WhenMovementDoesNotExist() throws Exception {
        // Arrange
        when(movementService.getMovementById(999L))
                .thenThrow(new ResourceNotFoundException("Inventory movement not found"));

        // Act + Assert
        mockMvc.perform(get("/api/inventory-movements/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Inventory movement not found"))
                .andExpect(jsonPath("$.path").value("/api/inventory-movements/999"));
    }

    @Test
    void shouldRecordMovementAndReturn201() throws Exception {
        // Arrange
        InventoryMovementDto dto = sampleMovementDto(MovementType.IN, 20, "Restock");
        when(movementService.recordMovement(any(CreateInventoryMovementRequest.class)))
                .thenReturn(dto);

        String requestBody = """
                {
                    "inventoryId": 100,
                    "movementType": "IN",
                    "quantity": 20,
                    "reason": "Restock"
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/inventory-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(500))
                .andExpect(jsonPath("$.inventoryId").value(100))
                .andExpect(jsonPath("$.movementType").value("IN"))
                .andExpect(jsonPath("$.quantity").value(20));
    }

    @Test
    void shouldReturn400WhenRecordMovementRequestHasZeroOrMissingFields() throws Exception {
        // Arrange: missing inventoryId and quantity = 0 (violates @Min(1))
        String invalidBody = """
                {
                    "movementType": "IN",
                    "quantity": 0
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/inventory-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.inventoryId").exists())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());

        verify(movementService, never()).recordMovement(any());
    }

    @Test
    void shouldReturn409WhenOutMovementExceedsAvailableStock() throws Exception {
        // Arrange
        when(movementService.recordMovement(any(CreateInventoryMovementRequest.class)))
                .thenThrow(new BusinessRuleException("Insufficient stock for OUT movement"));

        String requestBody = """
                {
                    "inventoryId": 100,
                    "movementType": "OUT",
                    "quantity": 100,
                    "reason": "Oversized shipment"
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/inventory-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Insufficient stock for OUT movement"));
    }
}

