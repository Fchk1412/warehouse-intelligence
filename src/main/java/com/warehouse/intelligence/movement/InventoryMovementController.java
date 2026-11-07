package com.warehouse.intelligence.movement;

import com.warehouse.intelligence.movement.dto.CreateInventoryMovementRequest;
import com.warehouse.intelligence.movement.dto.InventoryMovementDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory-movements")
public class InventoryMovementController {

    private final InventoryMovementService movementService;

    public InventoryMovementController(InventoryMovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping
    public Page<InventoryMovementDto> getAllMovements(
            @RequestParam(required = false) Long inventoryId,
            @RequestParam(required = false) MovementType movementType,
            @PageableDefault(size = 10) Pageable pageable) {
        return movementService.getAllMovements(inventoryId, movementType, pageable);
    }

    @GetMapping("/{id}")
    public InventoryMovementDto getMovementById(@PathVariable Long id) {
        return movementService.getMovementById(id);
    }

    @PostMapping
    public ResponseEntity<InventoryMovementDto> recordMovement(
            @Valid @RequestBody CreateInventoryMovementRequest request) {
        InventoryMovementDto movement = movementService.recordMovement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(movement);
    }
}

