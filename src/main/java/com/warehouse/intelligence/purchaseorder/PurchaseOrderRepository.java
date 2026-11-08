package com.warehouse.intelligence.purchaseorder;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"supplier", "warehouse", "items", "items.product"})
    Optional<PurchaseOrder> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"supplier", "warehouse", "items", "items.product"})
    @Query("SELECT po FROM PurchaseOrder po WHERE po.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"supplier", "warehouse", "items", "items.product"})
    @Query("""
           SELECT po FROM PurchaseOrder po
           WHERE (:supplierId IS NULL OR po.supplier.id = :supplierId)
           AND (:warehouseId IS NULL OR po.warehouse.id = :warehouseId)
           AND (:status IS NULL OR po.status = :status)
           """)
    Page<PurchaseOrder> findAllWithFilters(
            @Param("supplierId") Long supplierId,
            @Param("warehouseId") Long warehouseId,
            @Param("status") PurchaseOrderStatus status,
            Pageable pageable
    );
}

