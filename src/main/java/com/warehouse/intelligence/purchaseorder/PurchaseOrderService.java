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
import com.warehouse.intelligence.purchaseorder.dto.PurchaseOrderMapper;
import com.warehouse.intelligence.supplier.Supplier;
import com.warehouse.intelligence.supplier.SupplierProduct;
import com.warehouse.intelligence.supplier.SupplierProductRepository;
import com.warehouse.intelligence.supplier.SupplierRepository;
import com.warehouse.intelligence.warehouse.Warehouse;
import com.warehouse.intelligence.warehouse.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMovementService movementService;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            SupplierRepository supplierRepository,
            SupplierProductRepository supplierProductRepository,
            WarehouseRepository warehouseRepository,
            InventoryRepository inventoryRepository,
            InventoryMovementService movementService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.supplierProductRepository = supplierProductRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementService = movementService;
    }

    private PurchaseOrder findPurchaseOrderOrThrow(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found"));
    }

    private PurchaseOrder findPurchaseOrderForUpdateOrThrow(Long id) {
        return purchaseOrderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found"));
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderDto> getAllPurchaseOrders(
            Long supplierId,
            Long warehouseId,
            PurchaseOrderStatus status,
            Pageable pageable) {
        return purchaseOrderRepository
                .findAllWithFilters(supplierId, warehouseId, status, pageable)
                .map(PurchaseOrderMapper::toPurchaseOrderDto);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(Long id) {
        PurchaseOrder order = findPurchaseOrderOrThrow(id);
        return PurchaseOrderMapper.toPurchaseOrderDto(order);
    }

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(CreatePurchaseOrderRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        if (!supplier.isActive()) {
            throw new BusinessRuleException("Cannot create purchase order for an inactive supplier");
        }

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        if (!warehouse.isActive()) {
            throw new BusinessRuleException("Cannot create purchase order for an inactive warehouse");
        }

        Set<Long> seenProductIds = new HashSet<>();
        for (CreatePurchaseOrderItemRequest itemRequest : request.items()) {
            if (!seenProductIds.add(itemRequest.productId())) {
                throw new DuplicateResourceException(
                        "Duplicate product in purchase order items: " + itemRequest.productId()
                );
            }
        }

        PurchaseOrder order = new PurchaseOrder(supplier, warehouse);

        for (CreatePurchaseOrderItemRequest itemRequest : request.items()) {
            SupplierProduct offering = supplierProductRepository
                    .findBySupplierIdAndProductId(supplier.getId(), itemRequest.productId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Supplier does not offer product with id: " + itemRequest.productId()
                    ));

            if (!offering.isActive()) {
                throw new BusinessRuleException(
                        "Supplier product offering is inactive for product id: " + itemRequest.productId()
                );
            }

            Product product = offering.getProduct();
            if (!product.isActive()) {
                throw new BusinessRuleException("Cannot order an inactive product: " + product.getSku());
            }

            PurchaseOrderItem item = new PurchaseOrderItem(
                    product,
                    itemRequest.quantity(),
                    offering.getSupplyPrice()
            );
            order.addItem(item);
        }

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
        return PurchaseOrderMapper.toPurchaseOrderDto(savedOrder);
    }

    @Transactional
    public PurchaseOrderDto approvePurchaseOrder(Long id) {
        PurchaseOrder order = findPurchaseOrderForUpdateOrThrow(id);

        if (order.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new BusinessRuleException(
                    "Only PENDING purchase orders can be approved (current status: " + order.getStatus() + ")"
            );
        }

        if (!order.getSupplier().isActive()) {
            throw new BusinessRuleException("Cannot approve purchase order for an inactive supplier");
        }

        if (!order.getWarehouse().isActive()) {
            throw new BusinessRuleException("Cannot approve purchase order for an inactive warehouse");
        }

        for (PurchaseOrderItem item : order.getItems()) {
            if (!item.getProduct().isActive()) {
                throw new BusinessRuleException(
                        "Cannot approve purchase order containing inactive product: " + item.getProduct().getSku()
                );
            }
        }

        order.setStatus(PurchaseOrderStatus.APPROVED);
        return PurchaseOrderMapper.toPurchaseOrderDto(order);
    }

    @Transactional
    public PurchaseOrderDto cancelPurchaseOrder(Long id) {
        PurchaseOrder order = findPurchaseOrderForUpdateOrThrow(id);

        if (order.getStatus() == PurchaseOrderStatus.RECEIVED
                || order.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Cannot cancel a purchase order with status: " + order.getStatus()
            );
        }

        order.setStatus(PurchaseOrderStatus.CANCELLED);
        return PurchaseOrderMapper.toPurchaseOrderDto(order);
    }

    @Transactional
    public PurchaseOrderDto receivePurchaseOrder(Long id) {
        PurchaseOrder order = findPurchaseOrderForUpdateOrThrow(id);

        if (order.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Only APPROVED purchase orders can be received (current status: " + order.getStatus() + ")"
            );
        }

        Warehouse warehouse = order.getWarehouse();
        if (!warehouse.isActive()) {
            throw new BusinessRuleException("Cannot receive stock into an inactive warehouse");
        }

        for (PurchaseOrderItem item : order.getItems()) {
            Product product = item.getProduct();

            Inventory inventory = inventoryRepository
                    .findByProductIdAndWarehouseIdForUpdate(product.getId(), warehouse.getId())
                    .orElseGet(() -> inventoryRepository.save(
                            new Inventory(product, warehouse, 0, 0)
                    ));

            movementService.recordMovement(new CreateInventoryMovementRequest(
                    inventory.getId(),
                    MovementType.IN,
                    item.getQuantity(),
                    "Purchase order #" + order.getId()
            ));
        }

        order.setStatus(PurchaseOrderStatus.RECEIVED);
        order.setReceivedAt(Instant.now());

        return PurchaseOrderMapper.toPurchaseOrderDto(order);
    }
}

