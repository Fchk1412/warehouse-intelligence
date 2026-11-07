package com.warehouse.intelligence.supplier.dto;

import com.warehouse.intelligence.product.Product;
import com.warehouse.intelligence.supplier.Supplier;
import com.warehouse.intelligence.supplier.SupplierProduct;

public class SupplierProductMapper {

    public static SupplierProductDto toSupplierProductDto(SupplierProduct supplierProduct) {
        Supplier supplier = supplierProduct.getSupplier();
        Product product = supplierProduct.getProduct();

        return new SupplierProductDto(
                supplierProduct.getId(),
                supplier.getId(),
                supplier.getCode(),
                supplier.getName(),
                product.getId(),
                product.getSku(),
                product.getName(),
                supplierProduct.getSupplyPrice(),
                supplierProduct.getLeadTimeDays(),
                supplierProduct.isActive()
        );
    }
}

