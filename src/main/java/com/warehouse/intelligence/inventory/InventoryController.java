package com.warehouse.intelligence.inventory;

import com.warehouse.intelligence.inventory.dto.CreateInventoryRequest;
import com.warehouse.intelligence.inventory.dto.InventoryDto;
import com.warehouse.intelligence.inventory.dto.UpdateInventoryRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventories")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public Page<InventoryDto> getAllInventories(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @PageableDefault(size = 10) Pageable pageable) {
        return inventoryService.getAllInventories(productId, warehouseId, pageable);
    }

    @GetMapping("/{id}")
    public InventoryDto getInventoryById(@PathVariable Long id) {
        return inventoryService.getInventoryById(id);
    }

    @GetMapping("/low-stock")
    public Page<InventoryDto> getLowStockInventories(
            @PageableDefault(size = 10) Pageable pageable) {
        return inventoryService.getLowStockInventories(pageable);
    }

    @PostMapping
    public ResponseEntity<InventoryDto> createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {
        InventoryDto inventory = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(inventory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryDto> updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryRequest request) {
        InventoryDto inventory = inventoryService.updateInventory(id, request);
        return ResponseEntity.ok(inventory);
    }
}

