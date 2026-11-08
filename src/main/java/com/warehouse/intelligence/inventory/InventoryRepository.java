package com.warehouse.intelligence.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Override
    @EntityGraph(attributePaths = {"product", "warehouse"})
    Optional<Inventory> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("""
           SELECT i FROM Inventory i
           WHERE i.product.id = :productId
           AND i.warehouse.id = :warehouseId
           """)
    Optional<Inventory> findByProductIdAndWarehouseIdForUpdate(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId
    );

    /**
     * Creates an empty (quantity 0) inventory row if none exists for this product/warehouse.
     * A pessimistic lock can't protect a row that doesn't exist yet, so concurrent creators
     * rely on the unique constraint: the losing INSERT waits for the winner to commit and
     * then does nothing instead of failing. Callers should follow up with
     * findByProductIdAndWarehouseIdForUpdate to lock the (now guaranteed) row.
     *
     * @return 1 if a row was inserted, 0 if it already existed
     */
    @Modifying
    @Query(value = """
           INSERT INTO inventories (product_id, warehouse_id)
           VALUES (:productId, :warehouseId)
           ON CONFLICT (product_id, warehouse_id) DO NOTHING
           """, nativeQuery = true)
    int insertIfAbsent(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId
    );

    @Override
    @EntityGraph(attributePaths = {"product", "warehouse"})
    Page<Inventory> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    Page<Inventory> findByWarehouseId(Long warehouseId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    Page<Inventory> findByProductId(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    Page<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.minStockLevel")
    Page<Inventory> findLowStockInventories(Pageable pageable);
}

