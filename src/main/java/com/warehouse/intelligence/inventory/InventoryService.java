package com.warehouse.intelligence.inventory;

import com.warehouse.intelligence.common.exception.BusinessRuleException;
import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.inventory.dto.CreateInventoryRequest;
import com.warehouse.intelligence.inventory.dto.InventoryDto;
import com.warehouse.intelligence.inventory.dto.InventoryMapper;
import com.warehouse.intelligence.inventory.dto.UpdateInventoryRequest;
import com.warehouse.intelligence.movement.InventoryMovementService;
import com.warehouse.intelligence.movement.MovementType;
import com.warehouse.intelligence.movement.dto.CreateInventoryMovementRequest;
import com.warehouse.intelligence.product.Product;
import com.warehouse.intelligence.product.ProductRepository;
import com.warehouse.intelligence.warehouse.Warehouse;
import com.warehouse.intelligence.warehouse.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryMovementService movementService;

    public InventoryService(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            InventoryMovementService movementService) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.movementService = movementService;
    }

    private Inventory findInventoryOrThrow(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));
    }

    public Page<InventoryDto> getAllInventories(Long productId, Long warehouseId, Pageable pageable) {
        Page<Inventory> inventories;

        if (productId != null && warehouseId != null) {
            inventories = inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId, pageable);
        } else if (productId != null) {
            inventories = inventoryRepository.findByProductId(productId, pageable);
        } else if (warehouseId != null) {
            inventories = inventoryRepository.findByWarehouseId(warehouseId, pageable);
        } else {
            inventories = inventoryRepository.findAll(pageable);
        }

        return inventories.map(InventoryMapper::toInventoryDto);
    }

    public InventoryDto getInventoryById(Long id) {
        Inventory inventory = findInventoryOrThrow(id);
        return InventoryMapper.toInventoryDto(inventory);
    }

    public Page<InventoryDto> getLowStockInventories(Pageable pageable) {
        return inventoryRepository.findLowStockInventories(pageable)
                .map(InventoryMapper::toInventoryDto);
    }

    @Transactional
    public InventoryDto createInventory(CreateInventoryRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isActive()) {
            throw new BusinessRuleException("Cannot create inventory for an inactive product");
        }

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        if (!warehouse.isActive()) {
            throw new BusinessRuleException("Cannot create inventory for an inactive warehouse");
        }

        if (inventoryRepository.existsByProductIdAndWarehouseId(request.productId(), request.warehouseId())) {
            throw new DuplicateResourceException("Inventory record already exists for this product and warehouse");
        }

        // The record always starts at zero. Any opening stock goes through
        // InventoryMovementService, the single place allowed to change quantity.
        Inventory inventory = new Inventory(
                product,
                warehouse,
                0,
                request.minStockLevel()
        );

        Inventory savedInventory = inventoryRepository.save(inventory);

        if (request.quantity() > 0) {
            movementService.recordMovement(new CreateInventoryMovementRequest(
                    savedInventory.getId(),
                    MovementType.IN,
                    request.quantity(),
                    "Initial stock"
            ));
        }

        // savedInventory is a managed entity: the movement service loads the same
        // instance from the persistence context, so its quantity is already updated.
        return InventoryMapper.toInventoryDto(savedInventory);
    }

    @Transactional
    public InventoryDto updateInventory(Long id, UpdateInventoryRequest request) {
        Inventory inventory = findInventoryOrThrow(id);

        inventory.setMinStockLevel(request.minStockLevel());

        Inventory savedInventory = inventoryRepository.save(inventory);
        return InventoryMapper.toInventoryDto(savedInventory);
    }
}

