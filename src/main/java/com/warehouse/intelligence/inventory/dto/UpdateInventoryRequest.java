package com.warehouse.intelligence.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateInventoryRequest(
        @NotNull
        @Min(0)
        Integer quantity,

        @NotNull
        @Min(0)
        Integer minStockLevel
) {}

