package com.warehouse.intelligence.product;

import com.warehouse.intelligence.product.dto.CreateProductRequest;
import com.warehouse.intelligence.product.dto.ProductDto;
import com.warehouse.intelligence.product.dto.ProductMapper;
import com.warehouse.intelligence.product.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService( ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"
                ));
    }
    public Page<ProductDto> getAllProducts(Pageable pageable){
        return productRepository.findAll(pageable).map(ProductMapper::toProductDto);

    }

    public ProductDto getProductById(Long id) {

        Product product = findProductOrThrow(id);
        return ProductMapper.toProductDto(product);
    }

    public ProductDto createProduct(CreateProductRequest request){
        if (productRepository.existsBySku(request.sku())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "SKU already exists"
            );
        }

        Product product = new Product(
                request.sku(), request.name(), request.description(), request.unitPrice()
                );
        Product savedProduct = productRepository.save(product);
        return ProductMapper.toProductDto(savedProduct);

    }

    public ProductDto updateProduct(Long id, UpdateProductRequest request) {
            Product product = findProductOrThrow(id);

            if (productRepository.existsBySkuAndIdNot(request.sku(),id)){
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "SKU already exists"
                );}

            product.setSku(request.sku());
            product.setDescription(request.description());
            product.setName(request.name());
            product.setUnitPrice(request.unitPrice());

            Product savedProduct = productRepository.save(product);
            return ProductMapper.toProductDto(savedProduct);

    }

    public ProductDto deleteProduct(Long id){
        Product product = findProductOrThrow(id);
        product.setActive(false);
        productRepository.save(product);
        return ProductMapper.toProductDto(product);

    }
    }



