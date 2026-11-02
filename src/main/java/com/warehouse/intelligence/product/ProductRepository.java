package com.warehouse.intelligence.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {

    boolean existsBySkuAndIdNot(String sku, Long id);
}
