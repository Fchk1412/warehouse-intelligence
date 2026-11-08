package com.warehouse.intelligence.inventory;

import com.warehouse.intelligence.common.exception.BusinessRuleException;
import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.inventory.dto.CreateInventoryRequest;
import com.warehouse.intelligence.inventory.dto.InventoryDto;
import com.warehouse.intelligence.inventory.dto.UpdateInventoryRequest;
import com.warehouse.intelligence.movement.InventoryMovementService;
import com.warehouse.intelligence.movement.MovementType;
import com.warehouse.intelligence.movement.dto.CreateInventoryMovementRequest;
import com.warehouse.intelligence.product.Product;
import com.warehouse.intelligence.product.ProductRepository;
import com.warehouse.intelligence.warehouse.Warehouse;
import com.warehouse.intelligence.warehouse.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryMovementService movementService;

    @InjectMocks
    private InventoryService inventoryService;

    private Product activeProduct;
    private Warehouse activeWarehouse;

    @BeforeEach
    void setUp() {
        activeProduct = new Product(
                "LAPTOP-001",
                "Lenovo ThinkPad",
                "Business laptop",
                new BigDecimal("1299.99")
        );
        activeProduct.setId(1L);

        activeWarehouse = new Warehouse(
                "MOD-01",
                "Modena Central Warehouse",
                "Via Emilia Centro 100, Modena, Italy"
        );
        activeWarehouse.setId(2L);
    }

    @Test
    void shouldReturnInventoryWhenInventoryExists() {
        // Arrange
        Inventory inventory = new Inventory(activeProduct, activeWarehouse, 25, 10);
        inventory.setId(100L);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(inventory));

        // Act
        InventoryDto result = inventoryService.getInventoryById(100L);

        // Assert
        assertEquals(100L, result.id());
        assertEquals(1L, result.productId());
        assertEquals("LAPTOP-001", result.productSku());
        assertEquals("Lenovo ThinkPad", result.productName());
        assertEquals(2L, result.warehouseId());
        assertEquals("MOD-01", result.warehouseCode());
        assertEquals("Modena Central Warehouse", result.warehouseName());
        assertEquals(25, result.quantity());
        assertEquals(10, result.minStockLevel());
        assertFalse(result.lowStock());
    }

    @Test
    void shouldThrowNotFoundWhenInventoryDoesNotExist() {
        // Arrange
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> inventoryService.getInventoryById(999L)
        );

        assertEquals("Inventory record not found", exception.getMessage());
    }

    @Test
    void shouldReturnAllInventoriesWhenNoFiltersProvided() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Inventory inventory = new Inventory(activeProduct, activeWarehouse, 25, 10);
        Page<Inventory> page = new PageImpl<>(List.of(inventory));

        when(inventoryRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<InventoryDto> result = inventoryService.getAllInventories(null, null, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).productId());
        assertEquals(2L, result.getContent().get(0).warehouseId());
        verify(inventoryRepository).findAll(pageable);
    }

    @Test
    void shouldFilterInventoriesByProductIdOnly() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Inventory inventory = new Inventory(activeProduct, activeWarehouse, 25, 10);
        Page<Inventory> page = new PageImpl<>(List.of(inventory));

        when(inventoryRepository.findByProductId(1L, pageable)).thenReturn(page);

        // Act
        Page<InventoryDto> result = inventoryService.getAllInventories(1L, null, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).productId());
        assertEquals("LAPTOP-001", result.getContent().get(0).productSku());
        verify(inventoryRepository).findByProductId(1L, pageable);
    }

    @Test
    void shouldFilterInventoriesByWarehouseIdOnly() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Inventory inventory = new Inventory(activeProduct, activeWarehouse, 25, 10);
        Page<Inventory> page = new PageImpl<>(List.of(inventory));

        when(inventoryRepository.findByWarehouseId(2L, pageable)).thenReturn(page);

        // Act
        Page<InventoryDto> result = inventoryService.getAllInventories(null, 2L, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(2L, result.getContent().get(0).warehouseId());
        assertEquals("MOD-01", result.getContent().get(0).warehouseCode());
        verify(inventoryRepository).findByWarehouseId(2L, pageable);
    }

    @Test
    void shouldFilterInventoriesByBothProductIdAndWarehouseId() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Inventory inventory = new Inventory(activeProduct, activeWarehouse, 25, 10);
        Page<Inventory> page = new PageImpl<>(List.of(inventory));

        when(inventoryRepository.findByProductIdAndWarehouseId(1L, 2L, pageable)).thenReturn(page);

        // Act
        Page<InventoryDto> result = inventoryService.getAllInventories(1L, 2L, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).productId());
        assertEquals(2L, result.getContent().get(0).warehouseId());
        verify(inventoryRepository).findByProductIdAndWarehouseId(1L, 2L, pageable);
    }

    @Test
    void shouldReturnLowStockInventories() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Inventory lowStockInventory = new Inventory(activeProduct, activeWarehouse, 10, 10);
        Page<Inventory> page = new PageImpl<>(List.of(lowStockInventory));

        when(inventoryRepository.findLowStockInventories(pageable)).thenReturn(page);

        // Act
        Page<InventoryDto> result = inventoryService.getLowStockInventories(pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).lowStock());
        verify(inventoryRepository).findLowStockInventories(pageable);
    }

    @Test
    void shouldCreateInventoryAtZeroAndDelegateOpeningStockToMovementService() {
        // Arrange
        CreateInventoryRequest request = new CreateInventoryRequest(1L, 2L, 30, 10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(activeWarehouse));
        when(inventoryRepository.existsByProductIdAndWarehouseId(1L, 2L)).thenReturn(false);
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> {
                    Inventory saved = invocation.getArgument(0);
                    saved.setId(100L);
                    return saved;
                });

        // Act
        InventoryDto result = inventoryService.createInventory(request);

        // Assert: the record itself is persisted with quantity 0...
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        assertEquals(0, inventoryCaptor.getValue().getQuantity());
        assertEquals(10, inventoryCaptor.getValue().getMinStockLevel());

        // ...and the opening stock is recorded as an IN movement by the movement service
        ArgumentCaptor<CreateInventoryMovementRequest> movementCaptor =
                ArgumentCaptor.forClass(CreateInventoryMovementRequest.class);
        verify(movementService).recordMovement(movementCaptor.capture());
        assertEquals(100L, movementCaptor.getValue().inventoryId());
        assertEquals(MovementType.IN, movementCaptor.getValue().movementType());
        assertEquals(30, movementCaptor.getValue().quantity());
        assertEquals("Initial stock", movementCaptor.getValue().reason());

        assertEquals(1L, result.productId());
        assertEquals(2L, result.warehouseId());
    }

    @Test
    void shouldNotCreateInitialMovementWhenOpeningQuantityIsZero() {
        // Arrange
        CreateInventoryRequest request = new CreateInventoryRequest(1L, 2L, 0, 10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(activeWarehouse));
        when(inventoryRepository.existsByProductIdAndWarehouseId(1L, 2L)).thenReturn(false);
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InventoryDto result = inventoryService.createInventory(request);

        // Assert
        assertEquals(0, result.quantity());
        assertTrue(result.lowStock());
        verify(inventoryRepository).save(any(Inventory.class));
        verify(movementService, never()).recordMovement(any());
    }

    @Test
    void shouldThrowNotFoundWhenCreatingInventoryForNonExistentProduct() {
        // Arrange
        CreateInventoryRequest request = new CreateInventoryRequest(999L, 2L, 30, 10);

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> inventoryService.createInventory(request)
        );

        assertEquals("Product not found", exception.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenProductIsInactive() {
        // Arrange
        activeProduct.setActive(false);
        CreateInventoryRequest request = new CreateInventoryRequest(1L, 2L, 30, 10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));

        // Act + Assert
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> inventoryService.createInventory(request)
        );

        assertEquals("Cannot create inventory for an inactive product", exception.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void shouldThrowNotFoundWhenCreatingInventoryForNonExistentWarehouse() {
        // Arrange
        CreateInventoryRequest request = new CreateInventoryRequest(1L, 999L, 30, 10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> inventoryService.createInventory(request)
        );

        assertEquals("Warehouse not found", exception.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenWarehouseIsInactive() {
        // Arrange
        activeWarehouse.setActive(false);
        CreateInventoryRequest request = new CreateInventoryRequest(1L, 2L, 30, 10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(activeWarehouse));

        // Act + Assert
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> inventoryService.createInventory(request)
        );

        assertEquals("Cannot create inventory for an inactive warehouse", exception.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void shouldThrowConflictWhenInventoryRecordAlreadyExists() {
        // Arrange
        CreateInventoryRequest request = new CreateInventoryRequest(1L, 2L, 30, 10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(activeWarehouse));
        when(inventoryRepository.existsByProductIdAndWarehouseId(1L, 2L)).thenReturn(true);

        // Act + Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> inventoryService.createInventory(request)
        );

        assertEquals("Inventory record already exists for this product and warehouse", exception.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void shouldUpdateInventoryMinStockLevelAndSetLowStockTrue() {
        // Arrange
        Inventory existingInventory = new Inventory(activeProduct, activeWarehouse, 10, 5);
        existingInventory.setId(100L);

        UpdateInventoryRequest request = new UpdateInventoryRequest(15);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InventoryDto result = inventoryService.updateInventory(100L, request);

        // Assert
        assertEquals(10, result.quantity());
        assertEquals(15, result.minStockLevel());
        assertTrue(result.lowStock());
        verify(inventoryRepository).save(existingInventory);
    }

    @Test
    void shouldUpdateInventoryMinStockLevelBelowQuantityAndSetLowStockFalse() {
        // Arrange
        Inventory existingInventory = new Inventory(activeProduct, activeWarehouse, 10, 15);
        existingInventory.setId(100L);

        UpdateInventoryRequest request = new UpdateInventoryRequest(5);

        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InventoryDto result = inventoryService.updateInventory(100L, request);

        // Assert
        assertEquals(10, result.quantity());
        assertEquals(5, result.minStockLevel());
        assertFalse(result.lowStock());
        verify(inventoryRepository).save(existingInventory);
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingNonExistentInventory() {
        // Arrange
        UpdateInventoryRequest request = new UpdateInventoryRequest(15);

        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> inventoryService.updateInventory(999L, request)
        );

        assertEquals("Inventory record not found", exception.getMessage());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }
}

