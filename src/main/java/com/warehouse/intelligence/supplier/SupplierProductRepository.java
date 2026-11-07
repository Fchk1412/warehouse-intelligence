package com.warehouse.intelligence.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Long> {

    boolean existsBySupplierIdAndProductId(Long supplierId, Long productId);

    @EntityGraph(attributePaths = {"supplier", "product"})
    Optional<SupplierProduct> findBySupplierIdAndProductId(Long supplierId, Long productId);

    @Override
    @EntityGraph(attributePaths = {"supplier", "product"})
    Optional<SupplierProduct> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"supplier", "product"})
    Page<SupplierProduct> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"supplier", "product"})
    Page<SupplierProduct> findBySupplierId(Long supplierId, Pageable pageable);

    @EntityGraph(attributePaths = {"supplier", "product"})
    Page<SupplierProduct> findByProductId(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"supplier", "product"})
    Page<SupplierProduct> findBySupplierIdAndProductId(Long supplierId, Long productId, Pageable pageable);
}

