package com.warehouse.intelligence.inventory.dto;

import java.time.Instant;

public record InventoryDto(
        Long id,
        Long productId,
        String productSku,
        String productName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        int quantity,
        int minStockLevel,
        boolean lowStock,
        Instant updatedAt
) {}

