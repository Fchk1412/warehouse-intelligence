package com.warehouse.intelligence.movement.dto;

import com.warehouse.intelligence.inventory.Inventory;
import com.warehouse.intelligence.movement.InventoryMovement;
import com.warehouse.intelligence.product.Product;
import com.warehouse.intelligence.warehouse.Warehouse;

public class InventoryMovementMapper {

    public static InventoryMovementDto toInventoryMovementDto(InventoryMovement movement) {
        Inventory inventory = movement.getInventory();
        Product product = inventory.getProduct();
        Warehouse warehouse = inventory.getWarehouse();

        return new InventoryMovementDto(
                movement.getId(),
                inventory.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getReason(),
                movement.getCreatedAt()
        );
    }
}
