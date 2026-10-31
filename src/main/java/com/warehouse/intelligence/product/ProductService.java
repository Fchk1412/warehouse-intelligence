package com.warehouse.intelligence.product;

import com.warehouse.intelligence.product.dto.ProductDto;
import com.warehouse.intelligence.product.dto.ProductMapper;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService( ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductDto getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return ProductMapper.toProductDto(product);
    }
        
    }

