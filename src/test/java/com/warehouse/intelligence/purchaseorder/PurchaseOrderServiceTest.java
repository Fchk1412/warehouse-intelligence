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
import org.mockito.InOrder;
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
import static org.mockito.ArgumentMatchers.anyLong;
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

        laptopOffering = new SupplierProduct(activeSupplier, laptopProduct, new BigDecimal("1100.00"), 5);
        laptopOffering.setId(100L);

        dockOffering = new SupplierProduct(activeSupplier, dockProduct, new BigDecimal("150.00"), 4);
        dockOffering.setId(101L);
    }

    // ---------- helpers ----------

    private PurchaseOrder orderWithStatus(PurchaseOrderStatus status) {
        PurchaseOrder order = new PurchaseOrder(activeSupplier, activeWarehouse);
        order.setId(50L);
        order.setStatus(status);
        return order;
    }

    /** Stubs the lock-then-fetch sequence used by approve/cancel/receive. */
    private void stubLockedOrder(PurchaseOrder order) {
        when(purchaseOrderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findWithDetailsById(order.getId())).thenReturn(Optional.of(order));
    }

    // ---------- listing & retrieval ----------

    @Test
    void shouldReturnFilteredPurchaseOrdersAndFetchItemsForThatPageOnly() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.PENDING);
        order.addItem(new PurchaseOrderItem(laptopProduct, 20, new BigDecimal("1100.00")));
        Page<PurchaseOrder> page = new PageImpl<>(List.of(order));

        when(purchaseOrderRepository.findAllWithFilters(1L, 2L, PurchaseOrderStatus.PENDING, pageable))
                .thenReturn(page);

        // Act
        Page<PurchaseOrderDto> result = purchaseOrderService.getAllPurchaseOrders(
                1L, 2L, PurchaseOrderStatus.PENDING, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(50L, result.getContent().get(0).id());
        assertEquals(PurchaseOrderStatus.PENDING, result.getContent().get(0).status());
        assertEquals(1, result.getContent().get(0).items().size());
        verify(purchaseOrderRepository).findAllWithFilters(1L, 2L, PurchaseOrderStatus.PENDING, pageable);
        verify(purchaseOrderRepository).fetchItemsForOrders(List.of(50L));
    }

    @Test
    void shouldSkipItemFetchWhenPageIsEmpty() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(purchaseOrderRepository.findAllWithFilters(null, null, null, pageable))
                .thenReturn(Page.empty(pageable));

        // Act
        Page<PurchaseOrderDto> result = purchaseOrderService.getAllPurchaseOrders(null, null, null, pageable);

        // Assert
        assertTrue(result.isEmpty());
        verify(purchaseOrderRepository, never()).fetchItemsForOrders(any());
    }

    @Test
    void shouldThrowNotFoundWhenPurchaseOrderDoesNotExist() {
        when(purchaseOrderRepository.findWithDetailsById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.getPurchaseOrderById(999L));
    }

    // ---------- create ----------

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

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> purchaseOrderService.createPurchaseOrder(request));

        assertTrue(exception.getMessage().contains("Duplicate product"));
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenSupplierDoesNotOfferProduct() {
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                1L,
                2L,
                List.of(new CreatePurchaseOrderItemRequest(99L, 10))
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(activeSupplier));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(activeWarehouse));
        when(supplierProductRepository.findBySupplierIdAndProductId(1L, 99L))
                .thenReturn(Optional.empty());

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> purchaseOrderService.createPurchaseOrder(request));

        assertEquals("Supplier does not offer product with id: 99", exception.getMessage());
        verify(purchaseOrderRepository, never()).save(any());
    }

    // ---------- approve ----------

    @Test
    void shouldApprovePendingPurchaseOrderWhenAllEntitiesAreActive() {
        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.PENDING);
        order.addItem(new PurchaseOrderItem(laptopProduct, 20, new BigDecimal("1100.00")));
        stubLockedOrder(order);

        PurchaseOrderDto result = purchaseOrderService.approvePurchaseOrder(50L);

        assertEquals(PurchaseOrderStatus.APPROVED, result.status());
        assertEquals(PurchaseOrderStatus.APPROVED, order.getStatus());
    }

    @Test
    void shouldRejectApprovalWhenProductBecomesInactiveBeforeApproval() {
        laptopProduct.setActive(false);
        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.PENDING);
        order.addItem(new PurchaseOrderItem(laptopProduct, 20, new BigDecimal("1100.00")));
        stubLockedOrder(order);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> purchaseOrderService.approvePurchaseOrder(50L));

        assertTrue(exception.getMessage().contains("inactive product"));
        assertEquals(PurchaseOrderStatus.PENDING, order.getStatus());
    }

    @Test
    void shouldRejectApprovalWhenSupplierIsInactive() {
        activeSupplier.setActive(false);
        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.PENDING);
        stubLockedOrder(order);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> purchaseOrderService.approvePurchaseOrder(50L));

        assertEquals("Cannot approve purchase order for an inactive supplier", exception.getMessage());
        assertEquals(PurchaseOrderStatus.PENDING, order.getStatus());
    }

    @Test
    void shouldRejectApprovalWhenWarehouseIsInactive() {
        activeWarehouse.setActive(false);
        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.PENDING);
        stubLockedOrder(order);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> purchaseOrderService.approvePurchaseOrder(50L));

        assertEquals("Cannot approve purchase order for an inactive warehouse", exception.getMessage());
        assertEquals(PurchaseOrderStatus.PENDING, order.getStatus());
    }

    @Test
    void shouldRejectApprovingAnAlreadyApprovedOrder() {
        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.APPROVED);
        stubLockedOrder(order);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> purchaseOrderService.approvePurchaseOrder(50L));

        assertTrue(exception.getMessage().contains("Only PENDING"));
    }

    @Test
    void shouldThrowNotFoundWhenApprovingNonExistentOrder() {
        when(purchaseOrderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.approvePurchaseOrder(999L));
    }

    // ---------- cancel ----------

    @Test
    void shouldCancelApprovedPurchaseOrderSuccessfully() {
        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.APPROVED);
        stubLockedOrder(order);

        PurchaseOrderDto result = purchaseOrderService.cancelPurchaseOrder(50L);

        assertEquals(PurchaseOrderStatus.CANCELLED, result.status());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenCancellingAlreadyReceivedOrder() {
        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.RECEIVED);
        stubLockedOrder(order);

        assertThrows(BusinessRuleException.class,
                () -> purchaseOrderService.cancelPurchaseOrder(50L));
        assertEquals(PurchaseOrderStatus.RECEIVED, order.getStatus());
    }

    @Test
    void shouldThrowNotFoundWhenCancellingNonExistentOrder() {
        when(purchaseOrderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.cancelPurchaseOrder(999L));
    }

    // ---------- receive ----------

    @Test
    void shouldReceiveMultipleProductsCreatingMissingInventoryAndLockingInProductIdOrder() {
        // Arrange: laptop becomes inactive after approval — receipt must still succeed.
        laptopProduct.setActive(false);

        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.APPROVED);
        // Items deliberately added in reverse product-id order (dock=11 before laptop=10)
        order.addItem(new PurchaseOrderItem(dockProduct, 30, new BigDecimal("150.00")));
        order.addItem(new PurchaseOrderItem(laptopProduct, 20, new BigDecimal("1100.00")));
        stubLockedOrder(order);

        // Laptop: inventory already existed (insertIfAbsent is a no-op)
        Inventory existingLaptopInventory = new Inventory(laptopProduct, activeWarehouse, 5, 10);
        existingLaptopInventory.setId(200L);
        // Dock: inventory row is created by insertIfAbsent, starting at quantity 0
        Inventory newDockInventory = new Inventory(dockProduct, activeWarehouse, 0, 0);
        newDockInventory.setId(201L);

        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(10L, 2L))
                .thenReturn(Optional.of(existingLaptopInventory));
        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(11L, 2L))
                .thenReturn(Optional.of(newDockInventory));

        // Act
        PurchaseOrderDto result = purchaseOrderService.receivePurchaseOrder(50L);

        // Assert: order state
        assertEquals(PurchaseOrderStatus.RECEIVED, result.status());
        assertNotNull(result.receivedAt());

        // Assert: inventory rows processed in ascending product-id order (deadlock avoidance)
        InOrder lockOrder = inOrder(inventoryRepository);
        lockOrder.verify(inventoryRepository).insertIfAbsent(10L, 2L);
        lockOrder.verify(inventoryRepository).findByProductIdAndWarehouseIdForUpdate(10L, 2L);
        lockOrder.verify(inventoryRepository).insertIfAbsent(11L, 2L);
        lockOrder.verify(inventoryRepository).findByProductIdAndWarehouseIdForUpdate(11L, 2L);

        // Assert: PurchaseOrderService never saves Inventory itself
        verify(inventoryRepository, never()).save(any());

        // Assert: all stock changes delegated to the movement service
        ArgumentCaptor<CreateInventoryMovementRequest> captor =
                ArgumentCaptor.forClass(CreateInventoryMovementRequest.class);
        verify(movementService, times(2)).recordMovement(captor.capture());

        List<CreateInventoryMovementRequest> movements = captor.getAllValues();
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
        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.PENDING);
        stubLockedOrder(order);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> purchaseOrderService.receivePurchaseOrder(50L));

        assertTrue(exception.getMessage().contains("Only APPROVED"));
        verifyNoInteractions(inventoryRepository, movementService);
    }

    @Test
    void shouldRejectReceivingTheSameOrderTwice() {
        // The first receipt already moved the order to RECEIVED
        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.RECEIVED);
        order.addItem(new PurchaseOrderItem(laptopProduct, 20, new BigDecimal("1100.00")));
        stubLockedOrder(order);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> purchaseOrderService.receivePurchaseOrder(50L));

        assertTrue(exception.getMessage().contains("current status: RECEIVED"));
        verifyNoInteractions(inventoryRepository, movementService);
    }

    @Test
    void shouldRejectReceiveWhenDestinationWarehouseIsInactive() {
        activeWarehouse.setActive(false);
        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.APPROVED);
        stubLockedOrder(order);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> purchaseOrderService.receivePurchaseOrder(50L));

        assertEquals("Cannot receive stock into an inactive warehouse", exception.getMessage());
        verifyNoInteractions(inventoryRepository, movementService);
    }

    @Test
    void shouldThrowNotFoundWhenReceivingNonExistentOrder() {
        when(purchaseOrderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.receivePurchaseOrder(999L));
        verifyNoInteractions(inventoryRepository, movementService);
    }

    @Test
    void shouldPropagateFailureAndLeaveOrderApprovedWhenAMovementFails() {
        // Arrange
        PurchaseOrder order = orderWithStatus(PurchaseOrderStatus.APPROVED);
        order.addItem(new PurchaseOrderItem(laptopProduct, 20, new BigDecimal("1100.00")));
        order.addItem(new PurchaseOrderItem(dockProduct, 30, new BigDecimal("150.00")));
        stubLockedOrder(order);

        Inventory laptopInventory = new Inventory(laptopProduct, activeWarehouse, 5, 10);
        laptopInventory.setId(200L);
        Inventory dockInventory = new Inventory(dockProduct, activeWarehouse, 0, 0);
        dockInventory.setId(201L);

        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(anyLong(), anyLong()))
                .thenReturn(Optional.of(laptopInventory))
                .thenReturn(Optional.of(dockInventory));
        // First item succeeds, second item fails
        when(movementService.recordMovement(any()))
                .thenReturn(null)
                .thenThrow(new BusinessRuleException("Simulated failure on second item"));

        // Act + Assert
        assertThrows(BusinessRuleException.class,
                () -> purchaseOrderService.receivePurchaseOrder(50L));

        // The exception escapes the @Transactional method (so Spring rolls back),
        // and the order was never marked RECEIVED.
        assertEquals(PurchaseOrderStatus.APPROVED, order.getStatus());
        assertNull(order.getReceivedAt());
        verify(movementService, times(2)).recordMovement(any());
    }
}
