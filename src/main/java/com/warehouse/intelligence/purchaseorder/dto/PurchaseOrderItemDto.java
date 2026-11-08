package com.warehouse.intelligence.purchaseorder.dto;

import java.math.BigDecimal;

public record PurchaseOrderItemDto(
        Long id,
        Long productId,
        String productSku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}

