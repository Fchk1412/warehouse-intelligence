package com.warehouse.intelligence.warehouse;

import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.warehouse.dto.CreateWarehouseRequest;
import com.warehouse.intelligence.warehouse.dto.UpdateWarehouseRequest;
import com.warehouse.intelligence.warehouse.dto.WarehouseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(WarehouseController.class)
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WarehouseService warehouseService;

    private WarehouseDto sampleWarehouseDto() {
        return new WarehouseDto(
                1L,
                "MOD-01",
                "Modena Central Warehouse",
                "Via Emilia Centro 100, Modena, Italy",
                true
        );
    }

    @Test
    void shouldReturnPagedWarehouses() throws Exception {
        // Arrange
        Page<WarehouseDto> page = new PageImpl<>(List.of(sampleWarehouseDto()));
        when(warehouseService.getAllWarehouses(any(Pageable.class))).thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("MOD-01"))
                .andExpect(jsonPath("$.content[0].name").value("Modena Central Warehouse"))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    void shouldReturnWarehouseByIdWhenExists() throws Exception {
        // Arrange
        when(warehouseService.getWarehouseById(1L)).thenReturn(sampleWarehouseDto());

        // Act + Assert
        mockMvc.perform(get("/api/warehouses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("MOD-01"))
                .andExpect(jsonPath("$.location").value("Via Emilia Centro 100, Modena, Italy"));
    }

    @Test
    void shouldReturn404WhenWarehouseDoesNotExist() throws Exception {
        // Arrange
        when(warehouseService.getWarehouseById(999L))
                .thenThrow(new ResourceNotFoundException("Warehouse not found"));

        // Act + Assert
        mockMvc.perform(get("/api/warehouses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Warehouse not found"))
                .andExpect(jsonPath("$.path").value("/api/warehouses/999"));
    }

    @Test
    void shouldCreateWarehouseAndReturn201() throws Exception {
        // Arrange
        when(warehouseService.createWarehouse(any(CreateWarehouseRequest.class)))
                .thenReturn(sampleWarehouseDto());

        String requestBody = """
                {
                    "code": "MOD-01",
                    "name": "Modena Central Warehouse",
                    "location": "Via Emilia Centro 100, Modena, Italy"
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MOD-01"))
                .andExpect(jsonPath("$.name").value("Modena Central Warehouse"));
    }

    @Test
    void shouldReturn400WhenCreateWarehouseRequestIsInvalid() throws Exception {
        // Arrange: blank code and missing location
        String invalidBody = """
                {
                    "code": "",
                    "name": "Modena Central Warehouse"
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.code").exists())
                .andExpect(jsonPath("$.validationErrors.location").exists());

        verify(warehouseService, never()).createWarehouse(any());
    }

    @Test
    void shouldReturn409WhenCreatingWarehouseWithDuplicateCode() throws Exception {
        // Arrange
        when(warehouseService.createWarehouse(any(CreateWarehouseRequest.class)))
                .thenThrow(new DuplicateResourceException("Warehouse code already exists"));

        String requestBody = """
                {
                    "code": "MOD-01",
                    "name": "Modena Central Warehouse",
                    "location": "Via Emilia Centro 100, Modena, Italy"
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Warehouse code already exists"));
    }

    @Test
    void shouldUpdateWarehouseAndReturn200() throws Exception {
        // Arrange
        WarehouseDto updated = new WarehouseDto(
                1L,
                "MOD-02",
                "Modena Central Hub",
                "Via Emilia Centro 100, Modena, Italy",
                true
        );
        when(warehouseService.updateWarehouse(eq(1L), any(UpdateWarehouseRequest.class)))
                .thenReturn(updated);

        String requestBody = """
                {
                    "code": "MOD-02",
                    "name": "Modena Central Hub",
                    "location": "Via Emilia Centro 100, Modena, Italy"
                }
                """;

        // Act + Assert
        mockMvc.perform(put("/api/warehouses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MOD-02"))
                .andExpect(jsonPath("$.name").value("Modena Central Hub"));
    }

    @Test
    void shouldDeleteWarehouseAndReturn204() throws Exception {
        // Arrange
        WarehouseDto deactivated = new WarehouseDto(
                1L,
                "MOD-01",
                "Modena Central Warehouse",
                "Via Emilia Centro 100, Modena, Italy",
                false
        );
        when(warehouseService.deleteWarehouse(1L)).thenReturn(deactivated);

        // Act + Assert
        mockMvc.perform(delete("/api/warehouses/1"))
                .andExpect(status().isNoContent());

        verify(warehouseService).deleteWarehouse(1L);
    }
}

