package com.warehouse.intelligence.supplier;

import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.supplier.dto.CreateSupplierRequest;
import com.warehouse.intelligence.supplier.dto.SupplierDto;
import com.warehouse.intelligence.supplier.dto.UpdateSupplierRequest;
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
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier sampleSupplier() {
        Supplier supplier = new Supplier(
                "SUP-LENOVO",
                "Lenovo Italy S.r.l.",
                "orders@lenovo.it",
                "+39 02 1234567",
                "Via Milano 10, Milan, Italy"
        );
        supplier.setId(1L);
        return supplier;
    }

    @Test
    void shouldReturnPagedSuppliers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Supplier> page = new PageImpl<>(List.of(sampleSupplier()));

        when(supplierRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<SupplierDto> result = supplierService.getAllSuppliers(pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals("SUP-LENOVO", result.getContent().get(0).code());
        assertEquals("Lenovo Italy S.r.l.", result.getContent().get(0).name());
        verify(supplierRepository).findAll(pageable);
    }

    @Test
    void shouldReturnSupplierWhenSupplierExists() {
        // Arrange
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier()));

        // Act
        SupplierDto result = supplierService.getSupplierById(1L);

        // Assert
        assertEquals(1L, result.id());
        assertEquals("SUP-LENOVO", result.code());
        assertEquals("Lenovo Italy S.r.l.", result.name());
        assertEquals("orders@lenovo.it", result.email());
        assertEquals("+39 02 1234567", result.phone());
        assertEquals("Via Milano 10, Milan, Italy", result.address());
        assertTrue(result.active());
    }

    @Test
    void shouldThrowNotFoundWhenSupplierDoesNotExist() {
        // Arrange
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.getSupplierById(999L)
        );

        assertEquals("Supplier not found", exception.getMessage());
    }

    @Test
    void shouldCreateSupplierSuccessfully() {
        // Arrange
        CreateSupplierRequest request = new CreateSupplierRequest(
                "SUP-LENOVO",
                "Lenovo Italy S.r.l.",
                "orders@lenovo.it",
                "+39 02 1234567",
                "Via Milano 10, Milan, Italy"
        );

        when(supplierRepository.existsByCode("SUP-LENOVO")).thenReturn(false);
        when(supplierRepository.existsByEmail("orders@lenovo.it")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SupplierDto result = supplierService.createSupplier(request);

        // Assert
        assertEquals("SUP-LENOVO", result.code());
        assertEquals("Lenovo Italy S.r.l.", result.name());
        assertEquals("orders@lenovo.it", result.email());
        assertTrue(result.active());
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void shouldThrowConflictWhenCreatingSupplierWithDuplicateCode() {
        // Arrange
        CreateSupplierRequest request = new CreateSupplierRequest(
                "SUP-LENOVO",
                "Lenovo Italy S.r.l.",
                "orders@lenovo.it",
                null,
                null
        );

        when(supplierRepository.existsByCode("SUP-LENOVO")).thenReturn(true);

        // Act + Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> supplierService.createSupplier(request)
        );

        assertEquals("Supplier code already exists", exception.getMessage());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldThrowConflictWhenCreatingSupplierWithDuplicateEmail() {
        // Arrange
        CreateSupplierRequest request = new CreateSupplierRequest(
                "SUP-LENOVO",
                "Lenovo Italy S.r.l.",
                "orders@lenovo.it",
                null,
                null
        );

        when(supplierRepository.existsByCode("SUP-LENOVO")).thenReturn(false);
        when(supplierRepository.existsByEmail("orders@lenovo.it")).thenReturn(true);

        // Act + Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> supplierService.createSupplier(request)
        );

        assertEquals("Supplier email already exists", exception.getMessage());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldUpdateSupplierSuccessfully() {
        // Arrange
        Supplier existing = sampleSupplier();
        UpdateSupplierRequest request = new UpdateSupplierRequest(
                "SUP-LENOVO-IT",
                "Lenovo Enterprise Italy",
                "b2b@lenovo.it",
                "+39 02 7654321",
                "Via Roma 20, Milan, Italy"
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByCodeAndIdNot("SUP-LENOVO-IT", 1L)).thenReturn(false);
        when(supplierRepository.existsByEmailAndIdNot("b2b@lenovo.it", 1L)).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SupplierDto result = supplierService.updateSupplier(1L, request);

        // Assert
        assertEquals("SUP-LENOVO-IT", result.code());
        assertEquals("Lenovo Enterprise Italy", result.name());
        assertEquals("b2b@lenovo.it", result.email());
        verify(supplierRepository).save(existing);
    }

    @Test
    void shouldThrowConflictWhenUpdatingSupplierWithDuplicateCode() {
        // Arrange
        Supplier existing = sampleSupplier();
        UpdateSupplierRequest request = new UpdateSupplierRequest(
                "SUP-LOGITECH",
                "Lenovo Italy S.r.l.",
                "orders@lenovo.it",
                null,
                null
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByCodeAndIdNot("SUP-LOGITECH", 1L)).thenReturn(true);

        // Act + Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> supplierService.updateSupplier(1L, request)
        );

        assertEquals("Supplier code already exists", exception.getMessage());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldThrowConflictWhenUpdatingSupplierWithDuplicateEmail() {
        // Arrange
        Supplier existing = sampleSupplier();
        UpdateSupplierRequest request = new UpdateSupplierRequest(
                "SUP-LENOVO",
                "Lenovo Italy S.r.l.",
                "taken@other.it",
                null,
                null
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByCodeAndIdNot("SUP-LENOVO", 1L)).thenReturn(false);
        when(supplierRepository.existsByEmailAndIdNot("taken@other.it", 1L)).thenReturn(true);

        // Act + Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> supplierService.updateSupplier(1L, request)
        );

        assertEquals("Supplier email already exists", exception.getMessage());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldDeactivateSupplierWhenDeletingExistingSupplier() {
        // Arrange
        Supplier existing = sampleSupplier();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));

        // Act
        SupplierDto result = supplierService.deleteSupplier(1L);

        // Assert
        assertFalse(existing.isActive());
        assertFalse(result.active());
        verify(supplierRepository).save(existing);
    }

    @Test
    void shouldThrowNotFoundWhenDeletingNonExistentSupplier() {
        // Arrange
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.deleteSupplier(999L)
        );

        assertEquals("Supplier not found", exception.getMessage());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }
}

