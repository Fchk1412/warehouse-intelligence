package com.warehouse.intelligence.supplier;

import com.warehouse.intelligence.common.exception.BusinessRuleException;
import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.product.Product;
import com.warehouse.intelligence.product.ProductRepository;
import com.warehouse.intelligence.supplier.dto.CreateSupplierProductRequest;
import com.warehouse.intelligence.supplier.dto.SupplierProductDto;
import com.warehouse.intelligence.supplier.dto.SupplierProductMapper;
import com.warehouse.intelligence.supplier.dto.UpdateSupplierProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierProductService {

    private final SupplierProductRepository supplierProductRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public SupplierProductService(
            SupplierProductRepository supplierProductRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository) {
        this.supplierProductRepository = supplierProductRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
    }

    private SupplierProduct findSupplierProductOrThrow(Long id) {
        return supplierProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier product offering not found"));
    }

    @Transactional(readOnly = true)
    public Page<SupplierProductDto> getAllSupplierProducts(
            Long supplierId,
            Long productId,
            Pageable pageable) {
        Page<SupplierProduct> offerings;

        if (supplierId != null && productId != null) {
            offerings = supplierProductRepository.findBySupplierIdAndProductId(supplierId, productId, pageable);
        } else if (supplierId != null) {
            offerings = supplierProductRepository.findBySupplierId(supplierId, pageable);
        } else if (productId != null) {
            offerings = supplierProductRepository.findByProductId(productId, pageable);
        } else {
            offerings = supplierProductRepository.findAll(pageable);
        }

        return offerings.map(SupplierProductMapper::toSupplierProductDto);
    }

    @Transactional(readOnly = true)
    public SupplierProductDto getSupplierProductById(Long id) {
        SupplierProduct offering = findSupplierProductOrThrow(id);
        return SupplierProductMapper.toSupplierProductDto(offering);
    }

    @Transactional
    public SupplierProductDto createSupplierProduct(CreateSupplierProductRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        if (!supplier.isActive()) {
            throw new BusinessRuleException("Cannot register offering for an inactive supplier");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isActive()) {
            throw new BusinessRuleException("Cannot register offering for an inactive product");
        }

        if (supplierProductRepository.existsBySupplierIdAndProductId(request.supplierId(), request.productId())) {
            throw new DuplicateResourceException("Supplier already offers this product");
        }

        SupplierProduct offering = new SupplierProduct(
                supplier,
                product,
                request.supplyPrice(),
                request.leadTimeDays()
        );

        SupplierProduct savedOffering = supplierProductRepository.save(offering);
        return SupplierProductMapper.toSupplierProductDto(savedOffering);
    }

    @Transactional
    public SupplierProductDto updateSupplierProduct(Long id, UpdateSupplierProductRequest request) {
        SupplierProduct offering = findSupplierProductOrThrow(id);

        offering.setSupplyPrice(request.supplyPrice());
        offering.setLeadTimeDays(request.leadTimeDays());

        SupplierProduct savedOffering = supplierProductRepository.save(offering);
        return SupplierProductMapper.toSupplierProductDto(savedOffering);
    }

    @Transactional
    public SupplierProductDto deleteSupplierProduct(Long id) {
        SupplierProduct offering = findSupplierProductOrThrow(id);
        offering.setActive(false);
        supplierProductRepository.save(offering);
        return SupplierProductMapper.toSupplierProductDto(offering);
    }
}

