package com.warehouse.intelligence.warehouse;

import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.warehouse.dto.CreateWarehouseRequest;
import com.warehouse.intelligence.warehouse.dto.UpdateWarehouseRequest;
import com.warehouse.intelligence.warehouse.dto.WarehouseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    @Test
    void shouldReturnPagedWarehouses() {
        // Arrange
        Warehouse warehouse1 = new Warehouse(
                "MOD-01",
                "Modena Central Warehouse",
                "Via Emilia Centro 100, Modena, Italy"
        );
        Warehouse warehouse2 = new Warehouse(
                "MIL-01",
                "Milan North Warehouse",
                "Via Torino 45, Milan, Italy"
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Warehouse> warehousePage = new PageImpl<>(List.of(warehouse1, warehouse2));

        when(warehouseRepository.findAll(pageable)).thenReturn(warehousePage);

        // Act
        Page<WarehouseDto> result = warehouseService.getAllWarehouses(pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertEquals("MOD-01", result.getContent().get(0).code());
        assertEquals("MIL-01", result.getContent().get(1).code());
        verify(warehouseRepository).findAll(pageable);
    }

    @Test
    void shouldReturnWarehouseWhenWarehouseExists() {
        // Arrange
        Warehouse warehouse = new Warehouse(
                "MOD-01",
                "Modena Central Warehouse",
                "Via Emilia Centro 100, Modena, Italy"
        );

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

        // Act
        WarehouseDto result = warehouseService.getWarehouseById(1L);

        // Assert
        assertEquals("MOD-01", result.code());
        assertEquals("Modena Central Warehouse", result.name());
        assertEquals("Via Emilia Centro 100, Modena, Italy", result.location());
        assertTrue(result.active());
    }

    @Test
    void shouldThrowNotFoundWhenWarehouseDoesNotExist() {
        // Arrange
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.getWarehouseById(999L)
        );

        assertEquals("Warehouse not found", exception.getMessage());
    }

    @Test
    void shouldCreateWarehouseSuccessfully() {
        // Arrange
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "MOD-01",
                "Modena Central Warehouse",
                "Via Emilia Centro 100, Modena, Italy"
        );

        when(warehouseRepository.existsByCode("MOD-01")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WarehouseDto result = warehouseService.createWarehouse(request);

        // Assert
        assertEquals("MOD-01", result.code());
        assertEquals("Modena Central Warehouse", result.name());
        assertEquals("Via Emilia Centro 100, Modena, Italy", result.location());
        assertTrue(result.active());
        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void shouldThrowConflictWhenCreatingWarehouseWithDuplicateCode() {
        // Arrange
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "MOD-01",
                "Modena Central Warehouse",
                "Via Emilia Centro 100, Modena, Italy"
        );

        when(warehouseRepository.existsByCode("MOD-01")).thenReturn(true);

        // Act + Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> warehouseService.createWarehouse(request)
        );

        assertEquals("Warehouse code already exists", exception.getMessage());
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }

    @Test
    void shouldUpdateWarehouseSuccessfully() {
        // Arrange
        Warehouse existingWarehouse = new Warehouse(
                "MOD-01",
                "Modena Warehouse",
                "Via Emilia 10, Modena, Italy"
        );

        UpdateWarehouseRequest request = new UpdateWarehouseRequest(
                "MOD-02",
                "Modena Central Hub",
                "Via Emilia Centro 100, Modena, Italy"
        );

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existingWarehouse));
        when(warehouseRepository.existsByCodeAndIdNot("MOD-02", 1L)).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WarehouseDto result = warehouseService.updateWarehouse(1L, request);

        // Assert
        assertEquals("MOD-02", result.code());
        assertEquals("Modena Central Hub", result.name());
        assertEquals("Via Emilia Centro 100, Modena, Italy", result.location());
        verify(warehouseRepository).save(existingWarehouse);
    }

    @Test
    void shouldThrowConflictWhenUpdatingWarehouseWithDuplicateCode() {
        // Arrange
        Warehouse existingWarehouse = new Warehouse(
                "MOD-01",
                "Modena Warehouse",
                "Via Emilia 10, Modena, Italy"
        );

        UpdateWarehouseRequest request = new UpdateWarehouseRequest(
                "MIL-01",
                "Modena Central Hub",
                "Via Emilia Centro 100, Modena, Italy"
        );

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existingWarehouse));
        when(warehouseRepository.existsByCodeAndIdNot("MIL-01", 1L)).thenReturn(true);

        // Act + Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> warehouseService.updateWarehouse(1L, request)
        );

        assertEquals("Warehouse code already exists", exception.getMessage());
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingNonExistentWarehouse() {
        // Arrange
        UpdateWarehouseRequest request = new UpdateWarehouseRequest(
                "MOD-01",
                "Modena Central Hub",
                "Via Emilia Centro 100, Modena, Italy"
        );

        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.updateWarehouse(999L, request)
        );

        assertEquals("Warehouse not found", exception.getMessage());
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }

    @Test
    void shouldDeactivateWarehouseWhenWarehouseExists() {
        // Arrange
        Warehouse existingWarehouse = new Warehouse(
                "MOD-01",
                "Modena Central Warehouse",
                "Via Emilia Centro 100, Modena, Italy"
        );

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existingWarehouse));

        // Act
        WarehouseDto result = warehouseService.deleteWarehouse(1L);

        // Assert
        assertFalse(existingWarehouse.isActive());
        assertFalse(result.active());
        assertEquals("MOD-01", result.code());
        verify(warehouseRepository).save(existingWarehouse);
    }

    @Test
    void shouldThrowNotFoundWhenDeletingNonExistentWarehouse() {
        // Arrange
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.deleteWarehouse(999L)
        );

        assertEquals("Warehouse not found", exception.getMessage());
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }
}

