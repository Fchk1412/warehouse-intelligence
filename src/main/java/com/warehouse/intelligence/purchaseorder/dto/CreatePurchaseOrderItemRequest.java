package com.warehouse.intelligence.purchaseorder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreatePurchaseOrderItemRequest(
        @NotNull
        Long productId,

        @NotNull
        @Min(1)
        Integer quantity
) {}

