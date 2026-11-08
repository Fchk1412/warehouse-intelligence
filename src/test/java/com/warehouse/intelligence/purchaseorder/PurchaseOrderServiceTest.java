package com.warehouse.intelligence.purchaseorder;

import com.warehouse.intelligence.common.exception.BusinessRuleException;
import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.inventory.Inventory;
import com.warehouse.intelligence.inventory.InventoryRepository;
import com.warehouse.intelligence.movement.InventoryMovementService;
import com.warehouse.intelligence.movement.MovementType;
import com.warehouse.intelligence.movement.dto.CreateInventoryMovementRequest;
import com.warehouse.intelligence.product.Product;
import com.warehouse.intelligence.purchaseorder.dto.CreatePurchaseOrderItemRequest;
import com.warehouse.intelligence.purchaseorder.dto.CreatePurchaseOrderRequest;
import com.warehouse.intelligence.purchaseorder.dto.PurchaseOrderDto;
import com.warehouse.intelligence.supplier.Supplier;
import com.warehouse.intelligence.supplier.SupplierProduct;
import com.warehouse.intelligence.supplier.SupplierProductRepository;
import com.warehouse.intelligence.supplier.SupplierRepository;
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
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierProductRepository supplierProductRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryMovementService movementService;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private Supplier activeSupplier;
    private Warehouse activeWarehouse;
    private Product laptopProduct;
    private Product dockProduct;
    private SupplierProduct laptopOffering;
    private SupplierProduct dockOffering;

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

        activeWarehouse = new Warehouse(
                "MOD-01",
                "Modena Central Warehouse",
                "Via Emilia Centro 100, Modena, Italy"
        );
        activeWarehouse.setId(2L);

        laptopProduct = new Product(
                "LAPTOP-001",
                "Lenovo ThinkPad T14",
                "Business laptop",
                new BigDecimal("1299.99")
        );
        laptopProduct.setId(10L);

        dockProduct = new Product(
                "DOCK-001",
                "ThinkPad USB-C Dock",
                "Docking station",
                new BigDecimal("199.99")
        );
        dockProduct.setId(11L);

        laptopOffering = new SupplierProduct(
                activeSupplier,
                laptopProduct,
                new BigDecimal("1100.00"),
                5
        );
        laptopOffering.setId(100L);

        dockOffering = new SupplierProduct(
                activeSupplier,
                dockProduct,
                new BigDecimal("150.00"),
                4
        );
        dockOffering.setId(101L);
    }

    @Test
    void shouldReturnFilteredPurchaseOrdersWithCombinedCriteria() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        PurchaseOrder order = new PurchaseOrder(activeSupplier, activeWarehouse);
        order.setId(50L);
        order.addItem(new PurchaseOrderItem(laptopProduct, 20, new BigDecimal("1100.00")));
        Page<PurchaseOrder> page = new PageImpl<>(List.of(order));

        when(purchaseOrderRepository.findAllWithFilters(1L, 2L, PurchaseOrderStatus.PENDING, pageable))
                .thenReturn(page);

        // Act
        Page<PurchaseOrderDto> result = purchaseOrderService.getAllPurchaseOrders(
                1L,
                2L,
                PurchaseOrderStatus.PENDING,
                pageable
        );

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(50L, result.getContent().get(0).id());
        assertEquals(1L, result.getContent().get(0).supplierId());
        assertEquals(2L, result.getContent().get(0).warehouseId());
        assertEquals(PurchaseOrderStatus.PENDING, result.getContent().get(0).status());
        verify(purchaseOrderRepository).findAllWithFilters(1L, 2L, PurchaseOrderStatus.PENDING, pageable);
    }

    @Test
    void shouldCreatePurchaseOrderAndSnapshotSupplyPrices() {
        // Arrange
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                1L,
                2L,
                List.of(
                        new CreatePurchaseOrderItemRequest(10L, 20),
                        new CreatePurchaseOrderItemRequest(11L, 30)
                )
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(activeSupplier));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(activeWarehouse));
        when(supplierProductRepository.findBySupplierIdAndProductId(1L, 10L))
                .thenReturn(Optional.of(laptopOffering));
        when(supplierProductRepository.findBySupplierIdAndProductId(1L, 11L))
                .thenReturn(Optional.of(dockOffering));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PurchaseOrderDto result = purchaseOrderService.createPurchaseOrder(request);

        // Assert
        assertEquals(PurchaseOrderStatus.PENDING, result.status());
        assertEquals(2, result.items().size());
        assertEquals(new BigDecimal("1100.00"), result.items().get(0).unitPrice());
        assertEquals(new BigDecimal("22000.00"), result.items().get(0).lineTotal());
        assertEquals(new BigDecimal("150.00"), result.items().get(1).unitPrice());
        assertEquals(new BigDecimal("4500.00"), result.items().get(1).lineTotal());
        assertEquals(new BigDecimal("26500.00"), result.totalAmount());
        verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
    }

    @Test
    void shouldThrowConflictWhenCreatePurchaseOrderContainsDuplicateProductIds() {
        // Arrange
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                1L,
                2L,
                List.of(
                        new CreatePurchaseOrderItemRequest(10L, 10),
                        new CreatePurchaseOrderItemRequest(10L, 5)
                )
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(activeSupplier));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(activeWarehouse));

        // Act + Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> purchaseOrderService.createPurchaseOrder(request)
        );

        assertTrue(exception.getMessage().contains("Duplicate product"));
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenSupplierDoesNotOfferProduct() {
        // Arrange
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                1L,
                2L,
                List.of(new CreatePurchaseOrderItemRequest(99L, 10))
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(activeSupplier));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(activeWarehouse));
        when(supplierProductRepository.findBySupplierIdAndProductId(1L, 99L))
                .thenReturn(Optional.empty());

        // Act + Assert
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> purchaseOrderService.createPurchaseOrder(request)
        );

        assertEquals("Supplier does not offer product with id: 99", exception.getMessage());
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void shouldApprovePendingPurchaseOrderWhenAllEntitiesAreActive() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder(activeSupplier, activeWarehouse);
        order.setId(50L);
        order.addItem(new PurchaseOrderItem(laptopProduct, 20, new BigDecimal("1100.00")));

        when(purchaseOrderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(order));

        // Act
        PurchaseOrderDto result = purchaseOrderService.approvePurchaseOrder(50L);

        // Assert
        assertEquals(PurchaseOrderStatus.APPROVED, result.status());
        assertEquals(PurchaseOrderStatus.APPROVED, order.getStatus());
    }

    @Test
    void shouldRejectApprovalWhenProductBecomesInactiveBeforeApproval() {
        // Arrange
        laptopProduct.setActive(false);
        PurchaseOrder order = new PurchaseOrder(activeSupplier, activeWarehouse);
        order.setId(50L);
        order.addItem(new PurchaseOrderItem(laptopProduct, 20, new BigDecimal("1100.00")));

        when(purchaseOrderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(order));

        // Act + Assert
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> purchaseOrderService.approvePurchaseOrder(50L)
        );

        assertTrue(exception.getMessage().contains("inactive product"));
        assertEquals(PurchaseOrderStatus.PENDING, order.getStatus());
    }

    @Test
    void shouldCancelApprovedPurchaseOrderSuccessfully() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder(activeSupplier, activeWarehouse);
        order.setId(50L);
        order.setStatus(PurchaseOrderStatus.APPROVED);

        when(purchaseOrderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(order));

        // Act
        PurchaseOrderDto result = purchaseOrderService.cancelPurchaseOrder(50L);

        // Assert
        assertEquals(PurchaseOrderStatus.CANCELLED, result.status());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenCancellingAlreadyReceivedOrder() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder(activeSupplier, activeWarehouse);
        order.setId(50L);
        order.setStatus(PurchaseOrderStatus.RECEIVED);

        when(purchaseOrderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(order));

        // Act + Assert
        assertThrows(
                BusinessRuleException.class,
                () -> purchaseOrderService.cancelPurchaseOrder(50L)
        );
    }

    @Test
    void shouldReceiveApprovedOrderAndDelegateStockChangesToMovementService() {
        // Arrange: Product becomes inactive after approval — receipt should still succeed!
        laptopProduct.setActive(false);

        PurchaseOrder order = new PurchaseOrder(activeSupplier, activeWarehouse);
        order.setId(50L);
        order.setStatus(PurchaseOrderStatus.APPROVED);
        order.addItem(new PurchaseOrderItem(laptopProduct, 20, new BigDecimal("1100.00")));
        order.addItem(new PurchaseOrderItem(dockProduct, 30, new BigDecimal("150.00")));

        Inventory existingLaptopInventory = new Inventory(laptopProduct, activeWarehouse, 5, 10);
        existingLaptopInventory.setId(200L);

        when(purchaseOrderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(order));
        // Laptop already has an inventory row
        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(10L, 2L))
                .thenReturn(Optional.of(existingLaptopInventory));
        // Dock does not have an inventory row yet — created with initial quantity 0
        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(11L, 2L))
                .thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> {
                    Inventory created = invocation.getArgument(0);
                    created.setId(201L);
                    return created;
                });

        // Act
        PurchaseOrderDto result = purchaseOrderService.receivePurchaseOrder(50L);

        // Assert
        assertEquals(PurchaseOrderStatus.RECEIVED, result.status());
        assertNotNull(result.receivedAt());

        ArgumentCaptor<CreateInventoryMovementRequest> movementCaptor =
                ArgumentCaptor.forClass(CreateInventoryMovementRequest.class);
        verify(movementService, times(2)).recordMovement(movementCaptor.capture());

        List<CreateInventoryMovementRequest> movements = movementCaptor.getAllValues();
        assertEquals(200L, movements.get(0).inventoryId());
        assertEquals(MovementType.IN, movements.get(0).movementType());
        assertEquals(20, movements.get(0).quantity());
        assertEquals("Purchase order #50", movements.get(0).reason());

        assertEquals(201L, movements.get(1).inventoryId());
        assertEquals(MovementType.IN, movements.get(1).movementType());
        assertEquals(30, movements.get(1).quantity());
        assertEquals("Purchase order #50", movements.get(1).reason());
    }

    @Test
    void shouldRejectReceiveWhenOrderIsStillPending() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder(activeSupplier, activeWarehouse);
        order.setId(50L);
        order.setStatus(PurchaseOrderStatus.PENDING);

        when(purchaseOrderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(order));

        // Act + Assert
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> purchaseOrderService.receivePurchaseOrder(50L)
        );

        assertTrue(exception.getMessage().contains("Only APPROVED"));
        verify(movementService, never()).recordMovement(any());
    }

    @Test
    void shouldRejectReceiveWhenDestinationWarehouseIsInactive() {
        // Arrange
        activeWarehouse.setActive(false);
        PurchaseOrder order = new PurchaseOrder(activeSupplier, activeWarehouse);
        order.setId(50L);
        order.setStatus(PurchaseOrderStatus.APPROVED);

        when(purchaseOrderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(order));

        // Act + Assert
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> purchaseOrderService.receivePurchaseOrder(50L)
        );

        assertEquals("Cannot receive stock into an inactive warehouse", exception.getMessage());
        verify(movementService, never()).recordMovement(any());
    }

    @Test
    void shouldThrowNotFoundWhenPurchaseOrderDoesNotExist() {
        // Arrange
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> purchaseOrderService.getPurchaseOrderById(999L)
        );
    }
}

