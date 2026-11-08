package com.warehouse.intelligence.purchaseorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePurchaseOrderRequest(
        @NotNull
        Long supplierId,

        @NotNull
        Long warehouseId,

        @NotEmpty
        List<@Valid CreatePurchaseOrderItemRequest> items
) {}

