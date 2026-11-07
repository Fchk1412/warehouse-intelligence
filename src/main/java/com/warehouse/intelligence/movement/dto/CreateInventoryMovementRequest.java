package com.warehouse.intelligence.movement.dto;

import com.warehouse.intelligence.movement.MovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInventoryMovementRequest(
        @NotNull
        Long inventoryId,

        @NotNull
        MovementType movementType,

        @NotNull
        @Min(1)
        Integer quantity,

        @Size(max = 255)
        String reason
) {}
