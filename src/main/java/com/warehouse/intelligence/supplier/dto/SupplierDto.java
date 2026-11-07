package com.warehouse.intelligence.supplier.dto;

public record SupplierDto(
        Long id,
        String code,
        String name,
        String email,
        String phone,
        String address,
        boolean active
) {}

