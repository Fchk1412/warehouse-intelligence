package com.warehouse.intelligence.supplier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateSupplierProductRequest(
        @NotNull
        Long supplierId,

        @NotNull
        Long productId,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal supplyPrice,

        @Min(1)
        Integer leadTimeDays
) {}

