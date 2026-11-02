package com.warehouse.intelligence.product;


import com.warehouse.intelligence.product.dto.CreateProductRequest;
import com.warehouse.intelligence.product.dto.ProductDto;
import com.warehouse.intelligence.product.dto.UpdateProductRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {


    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Page<ProductDto> getAllProducts(@PageableDefault(size = 10) Pageable pageable) {
        return productService.getAllProducts(pageable);
    }

    @GetMapping("/{id}")
    public ProductDto getProductById(@PathVariable long id){
        return productService.getProductById(id);
    }
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        ProductDto product = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
           @PathVariable Long id, @Valid @RequestBody UpdateProductRequest request){

        ProductDto product = productService.updateProduct(id,request);
        return ResponseEntity.ok(product);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ProductDto> deleteProduct(@PathVariable long id){
        ProductDto product = productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

}
