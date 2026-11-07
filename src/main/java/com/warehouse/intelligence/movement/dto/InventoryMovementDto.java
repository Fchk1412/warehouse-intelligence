package com.warehouse.intelligence.movement.dto;

import com.warehouse.intelligence.movement.MovementType;

import java.time.Instant;

public record InventoryMovementDto(
        Long id,
        Long inventoryId,
        Long productId,
        String productSku,
        String productName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        MovementType movementType,
        int quantity,
        String reason,
        Instant createdAt
) {}
