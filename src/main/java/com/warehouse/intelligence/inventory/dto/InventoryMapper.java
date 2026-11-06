package com.warehouse.intelligence.inventory.dto;

import com.warehouse.intelligence.inventory.Inventory;
import com.warehouse.intelligence.product.Product;
import com.warehouse.intelligence.warehouse.Warehouse;

public class InventoryMapper {

    public static InventoryDto toInventoryDto(Inventory inventory) {
        Product product = inventory.getProduct();
        Warehouse warehouse = inventory.getWarehouse();
        boolean lowStock = inventory.getQuantity() <= inventory.getMinStockLevel();

        return new InventoryDto(
                inventory.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                inventory.getQuantity(),
                inventory.getMinStockLevel(),
                lowStock,
                inventory.getUpdatedAt()
        );
    }
}

