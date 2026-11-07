package com.warehouse.intelligence.supplier.dto;

import com.warehouse.intelligence.supplier.Supplier;

public class SupplierMapper {

    public static SupplierDto toSupplierDto(Supplier supplier) {
        return new SupplierDto(
                supplier.getId(),
                supplier.getCode(),
                supplier.getName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.isActive()
        );
    }
}

