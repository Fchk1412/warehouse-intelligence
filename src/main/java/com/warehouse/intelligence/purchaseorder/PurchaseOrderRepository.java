package com.warehouse.intelligence.purchaseorder;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    /**
     * Locks only the purchase_orders row (SELECT ... FOR UPDATE with no joins).
     * Relationships are loaded afterwards with {@link #findWithDetailsById(Long)}
     * inside the same transaction, which keeps the locking SQL simple and avoids
     * PostgreSQL's restriction on FOR UPDATE with the nullable side of outer joins.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM PurchaseOrder po WHERE po.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") Long id);

    /**
     * Loads one purchase order with supplier, warehouse, items and item products.
     * Not paginated, so fetching the items collection here is safe.
     */
    @Query("""
           SELECT DISTINCT po FROM PurchaseOrder po
           JOIN FETCH po.supplier
           JOIN FETCH po.warehouse
           LEFT JOIN FETCH po.items i
           LEFT JOIN FETCH i.product
           WHERE po.id = :id
           """)
    Optional<PurchaseOrder> findWithDetailsById(@Param("id") Long id);

    /**
     * Step 1 of paginated listing: headers only (plus the to-one supplier/warehouse).
     * No collection is fetched, so LIMIT/OFFSET is applied by the database.
     */
    @EntityGraph(attributePaths = {"supplier", "warehouse"})
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

    /**
     * Step 2 of paginated listing: one extra query that initialises the items
     * (and their products) for exactly the orders on the current page.
     */
    @Query("""
           SELECT DISTINCT po FROM PurchaseOrder po
           LEFT JOIN FETCH po.items i
           LEFT JOIN FETCH i.product
           WHERE po.id IN :ids
           """)
    List<PurchaseOrder> fetchItemsForOrders(@Param("ids") Collection<Long> ids);
}
