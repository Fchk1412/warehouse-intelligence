package com.warehouse.intelligence.purchaseorder;

import com.warehouse.intelligence.purchaseorder.dto.CreatePurchaseOrderRequest;
import com.warehouse.intelligence.purchaseorder.dto.PurchaseOrderDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping
    public Page<PurchaseOrderDto> getAllPurchaseOrders(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        return purchaseOrderService.getAllPurchaseOrders(supplierId, warehouseId, status, pageable);
    }

    @GetMapping("/{id}")
    public PurchaseOrderDto getPurchaseOrderById(@PathVariable Long id) {
        return purchaseOrderService.getPurchaseOrderById(id);
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderDto> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request) {
        PurchaseOrderDto order = purchaseOrderService.createPurchaseOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PurchaseOrderDto> approvePurchaseOrder(@PathVariable Long id) {
        PurchaseOrderDto order = purchaseOrderService.approvePurchaseOrder(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrderDto> cancelPurchaseOrder(@PathVariable Long id) {
        PurchaseOrderDto order = purchaseOrderService.cancelPurchaseOrder(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseOrderDto> receivePurchaseOrder(@PathVariable Long id) {
        PurchaseOrderDto order = purchaseOrderService.receivePurchaseOrder(id);
        return ResponseEntity.ok(order);
    }
}

