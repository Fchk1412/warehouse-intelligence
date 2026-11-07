package com.warehouse.intelligence.supplier.dto;

import java.math.BigDecimal;

public record SupplierProductDto(
        Long id,
        Long supplierId,
        String supplierCode,
        String supplierName,
        Long productId,
        String productSku,
        String productName,
        BigDecimal supplyPrice,
        Integer leadTimeDays,
        boolean active
) {}

