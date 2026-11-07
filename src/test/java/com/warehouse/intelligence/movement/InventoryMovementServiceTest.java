package com.warehouse.intelligence.movement;

import com.warehouse.intelligence.common.exception.BusinessRuleException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.inventory.Inventory;
import com.warehouse.intelligence.inventory.InventoryRepository;
import com.warehouse.intelligence.movement.dto.CreateInventoryMovementRequest;
import com.warehouse.intelligence.movement.dto.InventoryMovementDto;
import com.warehouse.intelligence.product.Product;
import com.warehouse.intelligence.warehouse.Warehouse;
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
class InventoryMovementServiceTest {

    @Mock
    private InventoryMovementRepository movementRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryMovementService movementService;

    private Product product;
    private Warehouse warehouse;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = new Product(
                "LAPTOP-001",
                "Lenovo ThinkPad",
                "Business laptop",
                new BigDecimal("1299.99")
        );
        product.setId(1L);

        warehouse = new Warehouse(
                "MOD-01",
                "Modena Central Warehouse",
                "Via Emilia Centro 100, Modena, Italy"
        );
        warehouse.setId(2L);

        inventory = new Inventory(product, warehouse, 25, 10);
        inventory.setId(100L);
    }

    @Test
    void shouldReturnMovementWhenMovementExists() {
        // Arrange
        InventoryMovement movement = new InventoryMovement(
                inventory,
                MovementType.IN,
                15,
                "Supplier delivery"
        );
        movement.setId(500L);

        when(movementRepository.findById(500L)).thenReturn(Optional.of(movement));

        // Act
        InventoryMovementDto result = movementService.getMovementById(500L);

        // Assert
        assertEquals(500L, result.id());
        assertEquals(100L, result.inventoryId());
        assertEquals(1L, result.productId());
        assertEquals("LAPTOP-001", result.productSku());
        assertEquals("Lenovo ThinkPad", result.productName());
        assertEquals(2L, result.warehouseId());
        assertEquals("MOD-01", result.warehouseCode());
        assertEquals("Modena Central Warehouse", result.warehouseName());
        assertEquals(MovementType.IN, result.movementType());
        assertEquals(15, result.quantity());
        assertEquals("Supplier delivery", result.reason());
    }

    @Test
    void shouldThrowNotFoundWhenMovementDoesNotExist() {
        // Arrange
        when(movementRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> movementService.getMovementById(999L)
        );

        assertEquals("Inventory movement not found", exception.getMessage());
    }

    @Test
    void shouldReturnAllMovementsWhenNoFiltersProvided() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        InventoryMovement movement = new InventoryMovement(inventory, MovementType.IN, 10, "Restock");
        Page<InventoryMovement> page = new PageImpl<>(List.of(movement));

        when(movementRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<InventoryMovementDto> result = movementService.getAllMovements(null, null, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(100L, result.getContent().get(0).inventoryId());
        verify(movementRepository).findAll(pageable);
    }

    @Test
    void shouldFilterMovementsByInventoryIdOnly() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        InventoryMovement movement = new InventoryMovement(inventory, MovementType.IN, 10, "Restock");
        Page<InventoryMovement> page = new PageImpl<>(List.of(movement));

        when(movementRepository.findByInventoryId(100L, pageable)).thenReturn(page);

        // Act
        Page<InventoryMovementDto> result = movementService.getAllMovements(100L, null, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(100L, result.getContent().get(0).inventoryId());
        verify(movementRepository).findByInventoryId(100L, pageable);
    }

    @Test
    void shouldFilterMovementsByMovementTypeOnly() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        InventoryMovement movement = new InventoryMovement(inventory, MovementType.OUT, 5, "Customer order");
        Page<InventoryMovement> page = new PageImpl<>(List.of(movement));

        when(movementRepository.findByMovementType(MovementType.OUT, pageable)).thenReturn(page);

        // Act
        Page<InventoryMovementDto> result = movementService.getAllMovements(null, MovementType.OUT, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(MovementType.OUT, result.getContent().get(0).movementType());
        verify(movementRepository).findByMovementType(MovementType.OUT, pageable);
    }

    @Test
    void shouldFilterMovementsByBothInventoryIdAndMovementType() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        InventoryMovement movement = new InventoryMovement(inventory, MovementType.OUT, 5, "Customer order");
        Page<InventoryMovement> page = new PageImpl<>(List.of(movement));

        when(movementRepository.findByInventoryIdAndMovementType(100L, MovementType.OUT, pageable))
                .thenReturn(page);

        // Act
        Page<InventoryMovementDto> result = movementService.getAllMovements(100L, MovementType.OUT, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(100L, result.getContent().get(0).inventoryId());
        assertEquals(MovementType.OUT, result.getContent().get(0).movementType());
        verify(movementRepository).findByInventoryIdAndMovementType(100L, MovementType.OUT, pageable);
    }

    @Test
    void shouldRecordInMovementAndIncreaseInventoryQuantity() {
        // Arrange
        CreateInventoryMovementRequest request = new CreateInventoryMovementRequest(
                100L,
                MovementType.IN,
                15,
                "Purchase order receipt"
        );

        when(inventoryRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(inventory));
        when(movementRepository.save(any(InventoryMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InventoryMovementDto result = movementService.recordMovement(request);

        // Assert
        assertEquals(40, inventory.getQuantity());
        assertEquals(100L, result.inventoryId());
        assertEquals(MovementType.IN, result.movementType());
        assertEquals(15, result.quantity());
        assertEquals("Purchase order receipt", result.reason());
        verify(movementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void shouldRecordOutMovementAndDecreaseInventoryQuantityWhenStockIsSufficient() {
        // Arrange
        CreateInventoryMovementRequest request = new CreateInventoryMovementRequest(
                100L,
                MovementType.OUT,
                10,
                "Customer shipment"
        );

        when(inventoryRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(inventory));
        when(movementRepository.save(any(InventoryMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InventoryMovementDto result = movementService.recordMovement(request);

        // Assert
        assertEquals(15, inventory.getQuantity());
        assertEquals(MovementType.OUT, result.movementType());
        assertEquals(10, result.quantity());
        assertEquals("Customer shipment", result.reason());
        verify(movementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void shouldAllowOutMovementBringingQuantityExactlyToZero() {
        // Arrange
        CreateInventoryMovementRequest request = new CreateInventoryMovementRequest(
                100L,
                MovementType.OUT,
                25,
                "Full clearance"
        );

        when(inventoryRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(inventory));
        when(movementRepository.save(any(InventoryMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InventoryMovementDto result = movementService.recordMovement(request);

        // Assert
        assertEquals(0, inventory.getQuantity());
        assertEquals(25, result.quantity());
        verify(movementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenOutMovementExceedsAvailableStock() {
        // Arrange
        CreateInventoryMovementRequest request = new CreateInventoryMovementRequest(
                100L,
                MovementType.OUT,
                30,
                "Oversized order"
        );

        when(inventoryRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(inventory));

        // Act + Assert
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> movementService.recordMovement(request)
        );

        assertEquals("Insufficient stock for OUT movement", exception.getMessage());
        assertEquals(25, inventory.getQuantity());
        verify(movementRepository, never()).save(any(InventoryMovement.class));
    }

    @Test
    void shouldThrowNotFoundWhenRecordingMovementForNonExistentInventory() {
        // Arrange
        CreateInventoryMovementRequest request = new CreateInventoryMovementRequest(
                999L,
                MovementType.IN,
                10,
                "Restock"
        );

        when(inventoryRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> movementService.recordMovement(request)
        );

        assertEquals("Inventory record not found", exception.getMessage());
        verify(movementRepository, never()).save(any(InventoryMovement.class));
    }
}

