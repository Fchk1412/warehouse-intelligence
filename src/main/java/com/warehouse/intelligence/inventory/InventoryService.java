package com.warehouse.intelligence.inventory;

import com.warehouse.intelligence.common.exception.BusinessRuleException;
import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.inventory.dto.CreateInventoryRequest;
import com.warehouse.intelligence.inventory.dto.InventoryDto;
import com.warehouse.intelligence.inventory.dto.InventoryMapper;
import com.warehouse.intelligence.inventory.dto.UpdateInventoryRequest;
import com.warehouse.intelligence.product.Product;
import com.warehouse.intelligence.product.ProductRepository;
import com.warehouse.intelligence.warehouse.Warehouse;
import com.warehouse.intelligence.warehouse.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    public InventoryService(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
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

        Inventory inventory = new Inventory(
                product,
                warehouse,
                request.quantity(),
                request.minStockLevel()
        );

        Inventory savedInventory = inventoryRepository.save(inventory);
        return InventoryMapper.toInventoryDto(savedInventory);
    }

    public InventoryDto updateInventory(Long id, UpdateInventoryRequest request) {
        Inventory inventory = findInventoryOrThrow(id);

        inventory.setQuantity(request.quantity());
        inventory.setMinStockLevel(request.minStockLevel());

        Inventory savedInventory = inventoryRepository.save(inventory);
        return InventoryMapper.toInventoryDto(savedInventory);
    }
}

