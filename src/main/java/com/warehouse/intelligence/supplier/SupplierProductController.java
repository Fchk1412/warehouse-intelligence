package com.warehouse.intelligence.supplier;

import com.warehouse.intelligence.supplier.dto.CreateSupplierProductRequest;
import com.warehouse.intelligence.supplier.dto.SupplierProductDto;
import com.warehouse.intelligence.supplier.dto.UpdateSupplierProductRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/supplier-products")
public class SupplierProductController {

    private final SupplierProductService supplierProductService;

    public SupplierProductController(SupplierProductService supplierProductService) {
        this.supplierProductService = supplierProductService;
    }

    @GetMapping
    public Page<SupplierProductDto> getAllSupplierProducts(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long productId,
            @PageableDefault(size = 10) Pageable pageable) {
        return supplierProductService.getAllSupplierProducts(supplierId, productId, pageable);
    }

    @GetMapping("/{id}")
    public SupplierProductDto getSupplierProductById(@PathVariable Long id) {
        return supplierProductService.getSupplierProductById(id);
    }

    @PostMapping
    public ResponseEntity<SupplierProductDto> createSupplierProduct(
            @Valid @RequestBody CreateSupplierProductRequest request) {
        SupplierProductDto offering = supplierProductService.createSupplierProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(offering);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierProductDto> updateSupplierProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplierProductRequest request) {
        SupplierProductDto offering = supplierProductService.updateSupplierProduct(id, request);
        return ResponseEntity.ok(offering);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplierProduct(@PathVariable Long id) {
        supplierProductService.deleteSupplierProduct(id);
        return ResponseEntity.noContent().build();
    }
}

