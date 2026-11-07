package com.warehouse.intelligence.supplier;

import com.warehouse.intelligence.supplier.dto.CreateSupplierRequest;
import com.warehouse.intelligence.supplier.dto.SupplierDto;
import com.warehouse.intelligence.supplier.dto.UpdateSupplierRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public Page<SupplierDto> getAllSuppliers(@PageableDefault(size = 10) Pageable pageable) {
        return supplierService.getAllSuppliers(pageable);
    }

    @GetMapping("/{id}")
    public SupplierDto getSupplierById(@PathVariable Long id) {
        return supplierService.getSupplierById(id);
    }

    @PostMapping
    public ResponseEntity<SupplierDto> createSupplier(
            @Valid @RequestBody CreateSupplierRequest request) {
        SupplierDto supplier = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(supplier);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierDto> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplierRequest request) {
        SupplierDto supplier = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(supplier);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}

