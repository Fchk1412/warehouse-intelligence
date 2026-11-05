package com.warehouse.intelligence.warehouse.dto;

public record WarehouseDto(
        Long id,
        String code,
        String name,
        String location,
        boolean active
) {}

