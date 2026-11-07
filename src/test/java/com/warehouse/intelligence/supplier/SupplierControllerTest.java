package com.warehouse.intelligence.supplier;

import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.supplier.dto.CreateSupplierRequest;
import com.warehouse.intelligence.supplier.dto.SupplierDto;
import com.warehouse.intelligence.supplier.dto.UpdateSupplierRequest;
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

@WebMvcTest(SupplierController.class)
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierService supplierService;

    private SupplierDto sampleSupplierDto() {
        return new SupplierDto(
                1L,
                "SUP-LENOVO",
                "Lenovo Italy S.r.l.",
                "orders@lenovo.it",
                "+39 02 1234567",
                "Via Milano 10, Milan, Italy",
                true
        );
    }

    @Test
    void shouldReturnPagedSuppliers() throws Exception {
        // Arrange
        Page<SupplierDto> page = new PageImpl<>(List.of(sampleSupplierDto()));
        when(supplierService.getAllSuppliers(any(Pageable.class))).thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("SUP-LENOVO"))
                .andExpect(jsonPath("$.content[0].email").value("orders@lenovo.it"));
    }

    @Test
    void shouldReturnSupplierByIdWhenExists() throws Exception {
        // Arrange
        when(supplierService.getSupplierById(1L)).thenReturn(sampleSupplierDto());

        // Act + Assert
        mockMvc.perform(get("/api/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("SUP-LENOVO"))
                .andExpect(jsonPath("$.name").value("Lenovo Italy S.r.l."))
                .andExpect(jsonPath("$.email").value("orders@lenovo.it"));
    }

    @Test
    void shouldReturn404WhenSupplierDoesNotExist() throws Exception {
        // Arrange
        when(supplierService.getSupplierById(999L))
                .thenThrow(new ResourceNotFoundException("Supplier not found"));

        // Act + Assert
        mockMvc.perform(get("/api/suppliers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Supplier not found"));
    }

    @Test
    void shouldCreateSupplierAndReturn201() throws Exception {
        // Arrange
        when(supplierService.createSupplier(any(CreateSupplierRequest.class)))
                .thenReturn(sampleSupplierDto());

        String requestBody = """
                {
                    "code": "SUP-LENOVO",
                    "name": "Lenovo Italy S.r.l.",
                    "email": "orders@lenovo.it",
                    "phone": "+39 02 1234567",
                    "address": "Via Milano 10, Milan, Italy"
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUP-LENOVO"))
                .andExpect(jsonPath("$.email").value("orders@lenovo.it"));
    }

    @Test
    void shouldReturn400WhenCreateSupplierHasInvalidEmailOrBlankCode() throws Exception {
        // Arrange
        String invalidBody = """
                {
                    "code": "",
                    "name": "Lenovo Italy S.r.l.",
                    "email": "not-an-email"
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.code").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());

        verify(supplierService, never()).createSupplier(any());
    }

    @Test
    void shouldReturn409WhenCreatingSupplierWithDuplicateCodeOrEmail() throws Exception {
        // Arrange
        when(supplierService.createSupplier(any(CreateSupplierRequest.class)))
                .thenThrow(new DuplicateResourceException("Supplier email already exists"));

        String requestBody = """
                {
                    "code": "SUP-LENOVO",
                    "name": "Lenovo Italy S.r.l.",
                    "email": "orders@lenovo.it"
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Supplier email already exists"));
    }

    @Test
    void shouldUpdateSupplierAndReturn200() throws Exception {
        // Arrange
        SupplierDto updated = new SupplierDto(
                1L,
                "SUP-LENOVO-IT",
                "Lenovo Enterprise Italy",
                "b2b@lenovo.it",
                "+39 02 7654321",
                "Via Roma 20, Milan, Italy",
                true
        );
        when(supplierService.updateSupplier(eq(1L), any(UpdateSupplierRequest.class)))
                .thenReturn(updated);

        String requestBody = """
                {
                    "code": "SUP-LENOVO-IT",
                    "name": "Lenovo Enterprise Italy",
                    "email": "b2b@lenovo.it",
                    "phone": "+39 02 7654321",
                    "address": "Via Roma 20, Milan, Italy"
                }
                """;

        // Act + Assert
        mockMvc.perform(put("/api/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUP-LENOVO-IT"))
                .andExpect(jsonPath("$.email").value("b2b@lenovo.it"));
    }

    @Test
    void shouldDeleteSupplierAndReturn204() throws Exception {
        // Arrange
        when(supplierService.deleteSupplier(1L)).thenReturn(sampleSupplierDto());

        // Act + Assert
        mockMvc.perform(delete("/api/suppliers/1"))
                .andExpect(status().isNoContent());

        verify(supplierService).deleteSupplier(1L);
    }
}

