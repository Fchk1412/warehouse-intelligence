package com.warehouse.intelligence.supplier;

import com.warehouse.intelligence.common.exception.BusinessRuleException;
import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.product.Product;
import com.warehouse.intelligence.product.ProductRepository;
import com.warehouse.intelligence.supplier.dto.CreateSupplierProductRequest;
import com.warehouse.intelligence.supplier.dto.SupplierProductDto;
import com.warehouse.intelligence.supplier.dto.UpdateSupplierProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierProductServiceTest {

    @Mock
    private SupplierProductRepository supplierProductRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private SupplierProductService supplierProductService;

    private Supplier activeSupplier;
    private Product activeProduct;

    @BeforeEach
    void setUp() {
        activeSupplier = new Supplier(
                "SUP-LENOVO",
                "Lenovo Italy S.r.l.",
                "orders@lenovo.it",
                "+39 02 1234567",
                "Via Milano 10, Milan, Italy"
        );
        activeSupplier.setId(1L);

        activeProduct = new Product(
                "LAPTOP-001",
                "Lenovo ThinkPad T14",
                "Business laptop",
                new BigDecimal("1299.99")
        );
        activeProduct.setId(2L);
    }

    @Test
    void shouldReturnSupplierProductWhenExists() {
        // Arrange
        SupplierProduct offering = new SupplierProduct(
                activeSupplier,
                activeProduct,
                new BigDecimal("1100.00"),
                5
        );
        offering.setId(10L);

        when(supplierProductRepository.findById(10L)).thenReturn(Optional.of(offering));

        // Act
        SupplierProductDto result = supplierProductService.getSupplierProductById(10L);

        // Assert
        assertEquals(10L, result.id());
        assertEquals(1L, result.supplierId());
        assertEquals("SUP-LENOVO", result.supplierCode());
        assertEquals("Lenovo Italy S.r.l.", result.supplierName());
        assertEquals(2L, result.productId());
        assertEquals("LAPTOP-001", result.productSku());
        assertEquals("Lenovo ThinkPad T14", result.productName());
        assertEquals(new BigDecimal("1100.00"), result.supplyPrice());
        assertEquals(5, result.leadTimeDays());
        assertTrue(result.active());
    }

    @Test
    void shouldThrowNotFoundWhenSupplierProductDoesNotExist() {
        // Arrange
        when(supplierProductRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> supplierProductService.getSupplierProductById(999L)
        );

        assertEquals("Supplier product offering not found", exception.getMessage());
    }

    @Test
    void shouldReturnAllSupplierProductsWhenNoFiltersProvided() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        SupplierProduct offering = new SupplierProduct(
                activeSupplier,
                activeProduct,
                new BigDecimal("1100.00"),
                5
        );
        Page<SupplierProduct> page = new PageImpl<>(List.of(offering));

        when(supplierProductRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<SupplierProductDto> result = supplierProductService.getAllSupplierProducts(null, null, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).supplierId());
        assertEquals(2L, result.getContent().get(0).productId());
        verify(supplierProductRepository).findAll(pageable);
    }

    @Test
    void shouldFilterSupplierProductsBySupplierIdOnly() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        SupplierProduct offering = new SupplierProduct(
                activeSupplier,
                activeProduct,
                new BigDecimal("1100.00"),
                5
        );
        Page<SupplierProduct> page = new PageImpl<>(List.of(offering));

        when(supplierProductRepository.findBySupplierId(1L, pageable)).thenReturn(page);

        // Act
        Page<SupplierProductDto> result = supplierProductService.getAllSupplierProducts(1L, null, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals("SUP-LENOVO", result.getContent().get(0).supplierCode());
        verify(supplierProductRepository).findBySupplierId(1L, pageable);
    }

    @Test
    void shouldFilterSupplierProductsByProductIdOnly() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        SupplierProduct offering = new SupplierProduct(
                activeSupplier,
                activeProduct,
                new BigDecimal("1100.00"),
                5
        );
        Page<SupplierProduct> page = new PageImpl<>(List.of(offering));

        when(supplierProductRepository.findByProductId(2L, pageable)).thenReturn(page);

        // Act
        Page<SupplierProductDto> result = supplierProductService.getAllSupplierProducts(null, 2L, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals("LAPTOP-001", result.getContent().get(0).productSku());
        verify(supplierProductRepository).findByProductId(2L, pageable);
    }

    @Test
    void shouldFilterSupplierProductsByBothSupplierIdAndProductId() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        SupplierProduct offering = new SupplierProduct(
                activeSupplier,
                activeProduct,
                new BigDecimal("1100.00"),
                5
        );
        Page<SupplierProduct> page = new PageImpl<>(List.of(offering));

        when(supplierProductRepository.findBySupplierIdAndProductId(1L, 2L, pageable)).thenReturn(page);

        // Act
        Page<SupplierProductDto> result = supplierProductService.getAllSupplierProducts(1L, 2L, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).supplierId());
        assertEquals(2L, result.getContent().get(0).productId());
        verify(supplierProductRepository).findBySupplierIdAndProductId(1L, 2L, pageable);
    }

    @Test
    void shouldCreateSupplierProductSuccessfully() {
        // Arrange
        CreateSupplierProductRequest request = new CreateSupplierProductRequest(
                1L,
                2L,
                new BigDecimal("1100.00"),
                5
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(activeSupplier));
        when(productRepository.findById(2L)).thenReturn(Optional.of(activeProduct));
        when(supplierProductRepository.existsBySupplierIdAndProductId(1L, 2L)).thenReturn(false);
        when(supplierProductRepository.save(any(SupplierProduct.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SupplierProductDto result = supplierProductService.createSupplierProduct(request);

        // Assert
        assertEquals(1L, result.supplierId());
        assertEquals("SUP-LENOVO", result.supplierCode());
        assertEquals(2L, result.productId());
        assertEquals("LAPTOP-001", result.productSku());
        assertEquals(new BigDecimal("1100.00"), result.supplyPrice());
        assertEquals(5, result.leadTimeDays());
        assertTrue(result.active());
        verify(supplierProductRepository).save(any(SupplierProduct.class));
    }

    @Test
    void shouldThrowNotFoundWhenCreatingOfferingForNonExistentSupplier() {
        // Arrange
        CreateSupplierProductRequest request = new CreateSupplierProductRequest(
                999L,
                2L,
                new BigDecimal("1100.00"),
                5
        );

        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> supplierProductService.createSupplierProduct(request)
        );

        assertEquals("Supplier not found", exception.getMessage());
        verify(supplierProductRepository, never()).save(any(SupplierProduct.class));
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenSupplierIsInactive() {
        // Arrange
        activeSupplier.setActive(false);
        CreateSupplierProductRequest request = new CreateSupplierProductRequest(
                1L,
                2L,
                new BigDecimal("1100.00"),
                5
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(activeSupplier));

        // Act + Assert
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> supplierProductService.createSupplierProduct(request)
        );

        assertEquals("Cannot register offering for an inactive supplier", exception.getMessage());
        verify(supplierProductRepository, never()).save(any(SupplierProduct.class));
    }

    @Test
    void shouldThrowNotFoundWhenCreatingOfferingForNonExistentProduct() {
        // Arrange
        CreateSupplierProductRequest request = new CreateSupplierProductRequest(
                1L,
                999L,
                new BigDecimal("1100.00"),
                5
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(activeSupplier));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> supplierProductService.createSupplierProduct(request)
        );

        assertEquals("Product not found", exception.getMessage());
        verify(supplierProductRepository, never()).save(any(SupplierProduct.class));
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenProductIsInactive() {
        // Arrange
        activeProduct.setActive(false);
        CreateSupplierProductRequest request = new CreateSupplierProductRequest(
                1L,
                2L,
                new BigDecimal("1100.00"),
                5
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(activeSupplier));
        when(productRepository.findById(2L)).thenReturn(Optional.of(activeProduct));

        // Act + Assert
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> supplierProductService.createSupplierProduct(request)
        );

        assertEquals("Cannot register offering for an inactive product", exception.getMessage());
        verify(supplierProductRepository, never()).save(any(SupplierProduct.class));
    }

    @Test
    void shouldThrowConflictWhenSupplierAlreadyOffersProduct() {
        // Arrange
        CreateSupplierProductRequest request = new CreateSupplierProductRequest(
                1L,
                2L,
                new BigDecimal("1100.00"),
                5
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(activeSupplier));
        when(productRepository.findById(2L)).thenReturn(Optional.of(activeProduct));
        when(supplierProductRepository.existsBySupplierIdAndProductId(1L, 2L)).thenReturn(true);

        // Act + Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> supplierProductService.createSupplierProduct(request)
        );

        assertEquals("Supplier already offers this product", exception.getMessage());
        verify(supplierProductRepository, never()).save(any(SupplierProduct.class));
    }

    @Test
    void shouldUpdateSupplierProductSuccessfully() {
        // Arrange
        SupplierProduct existing = new SupplierProduct(
                activeSupplier,
                activeProduct,
                new BigDecimal("1100.00"),
                5
        );
        existing.setId(10L);

        UpdateSupplierProductRequest request = new UpdateSupplierProductRequest(
                new BigDecimal("1050.00"),
                7
        );

        when(supplierProductRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(supplierProductRepository.save(any(SupplierProduct.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SupplierProductDto result = supplierProductService.updateSupplierProduct(10L, request);

        // Assert
        assertEquals(new BigDecimal("1050.00"), result.supplyPrice());
        assertEquals(7, result.leadTimeDays());
        verify(supplierProductRepository).save(existing);
    }

    @Test
    void shouldDeactivateSupplierProductWhenDeleting() {
        // Arrange
        SupplierProduct existing = new SupplierProduct(
                activeSupplier,
                activeProduct,
                new BigDecimal("1100.00"),
                5
        );
        existing.setId(10L);

        when(supplierProductRepository.findById(10L)).thenReturn(Optional.of(existing));

        // Act
        SupplierProductDto result = supplierProductService.deleteSupplierProduct(10L);

        // Assert
        assertFalse(existing.isActive());
        assertFalse(result.active());
        verify(supplierProductRepository).save(existing);
    }
}

