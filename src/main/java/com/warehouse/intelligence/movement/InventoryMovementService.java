package com.warehouse.intelligence.movement;

import com.warehouse.intelligence.common.exception.BusinessRuleException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.inventory.Inventory;
import com.warehouse.intelligence.inventory.InventoryRepository;
import com.warehouse.intelligence.movement.dto.CreateInventoryMovementRequest;
import com.warehouse.intelligence.movement.dto.InventoryMovementDto;
import com.warehouse.intelligence.movement.dto.InventoryMovementMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryMovementService {

    private final InventoryMovementRepository movementRepository;
    private final InventoryRepository inventoryRepository;

    public InventoryMovementService(
            InventoryMovementRepository movementRepository,
            InventoryRepository inventoryRepository) {
        this.movementRepository = movementRepository;
        this.inventoryRepository = inventoryRepository;
    }

    private InventoryMovement findMovementOrThrow(Long id) {
        return movementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory movement not found"));
    }

    @Transactional(readOnly = true)
    public Page<InventoryMovementDto> getAllMovements(
            Long inventoryId,
            MovementType movementType,
            Pageable pageable) {
        Page<InventoryMovement> movements;

        if (inventoryId != null && movementType != null) {
            movements = movementRepository.findByInventoryIdAndMovementType(inventoryId, movementType, pageable);
        } else if (inventoryId != null) {
            movements = movementRepository.findByInventoryId(inventoryId, pageable);
        } else if (movementType != null) {
            movements = movementRepository.findByMovementType(movementType, pageable);
        } else {
            movements = movementRepository.findAll(pageable);
        }

        return movements.map(InventoryMovementMapper::toInventoryMovementDto);
    }

    @Transactional(readOnly = true)
    public InventoryMovementDto getMovementById(Long id) {
        InventoryMovement movement = findMovementOrThrow(id);
        return InventoryMovementMapper.toInventoryMovementDto(movement);
    }

    @Transactional
    public InventoryMovementDto recordMovement(CreateInventoryMovementRequest request) {
        Inventory inventory = inventoryRepository.findByIdForUpdate(request.inventoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

        switch (request.movementType()) {
            case IN -> inventory.setQuantity(
                    inventory.getQuantity() + request.quantity()
            );

            case OUT -> {
                if (inventory.getQuantity() < request.quantity()) {
                    throw new BusinessRuleException("Insufficient stock for OUT movement");
                }

                inventory.setQuantity(
                        inventory.getQuantity() - request.quantity()
                );
            }
        }

        InventoryMovement movement = new InventoryMovement(
                inventory,
                request.movementType(),
                request.quantity(),
                request.reason()
        );

        InventoryMovement savedMovement = movementRepository.save(movement);
        return InventoryMovementMapper.toInventoryMovementDto(savedMovement);
    }
}

