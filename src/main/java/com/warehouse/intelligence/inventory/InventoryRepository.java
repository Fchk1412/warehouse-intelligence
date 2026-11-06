package com.warehouse.intelligence.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Override
    @EntityGraph(attributePaths = {"product", "warehouse"})
    Optional<Inventory> findById(Long id);

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

