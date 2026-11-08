package com.warehouse.intelligence.purchaseorder.dto;

import com.warehouse.intelligence.purchaseorder.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PurchaseOrderDto(
        Long id,
        Long supplierId,
        String supplierCode,
        String supplierName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        PurchaseOrderStatus status,
        List<PurchaseOrderItemDto> items,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant receivedAt
) {}

