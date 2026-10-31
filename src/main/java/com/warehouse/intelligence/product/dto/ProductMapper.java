package com.warehouse.intelligence.product.dto;

import com.warehouse.intelligence.product.Product;

public class ProductMapper {

    public static ProductDto toProductDto(Product product){
        return new ProductDto(
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getUnitPrice()
        );
    }
}
