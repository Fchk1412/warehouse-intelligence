package com.warehouse.intelligence.supplier;

import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.supplier.dto.CreateSupplierRequest;
import com.warehouse.intelligence.supplier.dto.SupplierDto;
import com.warehouse.intelligence.supplier.dto.SupplierMapper;
import com.warehouse.intelligence.supplier.dto.UpdateSupplierRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    private Supplier findSupplierOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto> getAllSuppliers(Pageable pageable) {
        return supplierRepository.findAll(pageable).map(SupplierMapper::toSupplierDto);
    }

    @Transactional(readOnly = true)
    public SupplierDto getSupplierById(Long id) {
        Supplier supplier = findSupplierOrThrow(id);
        return SupplierMapper.toSupplierDto(supplier);
    }

    @Transactional
    public SupplierDto createSupplier(CreateSupplierRequest request) {
        if (supplierRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Supplier code already exists");
        }

        if (supplierRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Supplier email already exists");
        }

        Supplier supplier = new Supplier(
                request.code(),
                request.name(),
                request.email(),
                request.phone(),
                request.address()
        );

        Supplier savedSupplier = supplierRepository.save(supplier);
        return SupplierMapper.toSupplierDto(savedSupplier);
    }

    @Transactional
    public SupplierDto updateSupplier(Long id, UpdateSupplierRequest request) {
        Supplier supplier = findSupplierOrThrow(id);

        if (supplierRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new DuplicateResourceException("Supplier code already exists");
        }

        if (supplierRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException("Supplier email already exists");
        }

        supplier.setCode(request.code());
        supplier.setName(request.name());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());

        Supplier savedSupplier = supplierRepository.save(supplier);
        return SupplierMapper.toSupplierDto(savedSupplier);
    }

    @Transactional
    public SupplierDto deleteSupplier(Long id) {
        Supplier supplier = findSupplierOrThrow(id);
        supplier.setActive(false);
        supplierRepository.save(supplier);
        return SupplierMapper.toSupplierDto(supplier);
    }
}

