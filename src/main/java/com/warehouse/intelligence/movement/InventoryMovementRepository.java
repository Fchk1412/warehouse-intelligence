package com.warehouse.intelligence.movement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    @Override
    @EntityGraph(attributePaths = {"inventory", "inventory.product", "inventory.warehouse"})
    Optional<InventoryMovement> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"inventory", "inventory.product", "inventory.warehouse"})
    Page<InventoryMovement> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"inventory", "inventory.product", "inventory.warehouse"})
    Page<InventoryMovement> findByInventoryId(Long inventoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"inventory", "inventory.product", "inventory.warehouse"})
    Page<InventoryMovement> findByMovementType(MovementType movementType, Pageable pageable);

    @EntityGraph(attributePaths = {"inventory", "inventory.product", "inventory.warehouse"})
    Page<InventoryMovement> findByInventoryIdAndMovementType(
            Long inventoryId,
            MovementType movementType,
            Pageable pageable
    );
}

