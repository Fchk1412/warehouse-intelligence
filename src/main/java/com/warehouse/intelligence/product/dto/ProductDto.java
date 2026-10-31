package com.warehouse.intelligence.product.dto;

import java.math.BigDecimal;

public record ProductDto(
        String name,
        String sku,
        String description,
        BigDecimal unitPrice
) {}