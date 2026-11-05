package com.warehouse.intelligence.warehouse.dto;

import com.warehouse.intelligence.warehouse.Warehouse;

public class WarehouseMapper {

    public static WarehouseDto toWarehouseDto(Warehouse warehouse) {
        return new WarehouseDto(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getLocation(),
                warehouse.isActive()
        );
    }
}

