package com.warehouse.intelligence.product;

import com.warehouse.intelligence.common.exception.DuplicateResourceException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.product.dto.CreateProductRequest;
import com.warehouse.intelligence.product.dto.ProductDto;
import com.warehouse.intelligence.product.dto.UpdateProductRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;


    @Test
    void shouldReturnProductWhenProductExists() {

        // Arrange
        Product product = new Product(
                "LAPTOP-001",
                "Lenovo ThinkPad",
                "Business laptop",
                new BigDecimal("1299.99")
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        // Act
        ProductDto result = productService.getProductById(1L);

        // Assert
        assertEquals("LAPTOP-001", result.sku());
        assertEquals("Lenovo ThinkPad", result.name());
    }

    @Test
    void shouldThrowNotFoundWhenProductDoesNotExist() {

        // Arrange
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(999L)
        );

        assertEquals("Product not found", exception.getMessage());
    }

    @Test
    void shouldCreateProductSuccessfully() {

        // Arrange
        CreateProductRequest request = new CreateProductRequest(
                "LAPTOP-002",
                "Lenovo ThinkPad",
                "Business laptop",
                new BigDecimal("1299.99")
        );

        when(productRepository.existsBySku("LAPTOP-002")).thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductDto result = productService.createProduct(request);

        // Assert
        assertEquals("LAPTOP-002", result.sku());
        assertEquals("Lenovo ThinkPad", result.name());

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldThrowConflictWhenCreatingProductWithDuplicateSku() {

        // Arrange
        CreateProductRequest request = new CreateProductRequest(
                "LAPTOP-001",
                "Lenovo ThinkPad",
                "Business laptop",
                new BigDecimal("1299.99")
        );

        when(productRepository.existsBySku("LAPTOP-001")).thenReturn(true);

        // Act + Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> productService.createProduct(request)
        );

        assertEquals("SKU already exists", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldUpdateProductSuccessfully() {

        //Arrange

        Product existingProduct = new Product(
                "LAPTOP-001",
                "Lenovo ThinkPad",
                "Business laptop",
                new BigDecimal("1299.99")
        );

        UpdateProductRequest request = new UpdateProductRequest(
                "LAPTOP-002",
                "Lenovo ThinkPad",
                "Business laptop",
                new BigDecimal("1499.99"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        when(productRepository.existsBySkuAndIdNot("LAPTOP-002", 1L)).thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


        //Act

        ProductDto result = productService.updateProduct(1L, request);

        //Assert

        assertEquals("LAPTOP-002", result.sku());
        assertEquals(new BigDecimal("1499.99"), result.unitPrice());

        verify(productRepository).save(any(Product.class));


    }

    @Test
    void shouldUpdateProductUnsuccessfully() {
        //Arrange
        Product existingProduct = new Product(
                "LAPTOP-001",
                "Lenovo ThinkPad",
                "Business laptop",
                new BigDecimal("1299.99")
        );

        UpdateProductRequest request = new UpdateProductRequest(
                "LAPTOP-002",
                "Updated Lenovo ThinkPad",
                "New Business laptop",
                new BigDecimal("1499.99"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        when(productRepository.existsBySkuAndIdNot("LAPTOP-002", 1L)).thenReturn(true);

        //Act
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> productService.updateProduct(1L,request)
        );

        //Assert
        assertEquals("SKU already exists", exception.getMessage());
        verify(productRepository,never()).save(any(Product.class));
    }

    @Test
    void shouldDeactivateProductWhenProductExists() {
        // Arrange
        Product existingProduct = new Product(
                "LAPTOP-001",
                "Lenovo ThinkPad",
                "Business laptop",
                new BigDecimal("1299.99")
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        // Act
        ProductDto result = productService.deleteProduct(1L);

        // Assert
        assertFalse(existingProduct.isActive());
        assertEquals("LAPTOP-001", result.sku());
        verify(productRepository).save(existingProduct);
    }

    @Test
    void shouldThrowNotFoundWhenDeletingNonExistentProduct() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.deleteProduct(999L)
        );

        assertEquals("Product not found", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldReturnPagedProducts() {
        // Arrange
        Product product1 = new Product(
                "LAPTOP-001",
                "Lenovo ThinkPad",
                "Business laptop",
                new BigDecimal("1299.99")
        );
        Product product2 = new Product(
                "MOUSE-001",
                "Logitech MX Master",
                "Wireless mouse",
                new BigDecimal("99.99")
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product1, product2));

        when(productRepository.findAll(pageable)).thenReturn(productPage);

        // Act
        Page<ProductDto> result = productService.getAllProducts(pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertEquals("LAPTOP-001", result.getContent().get(0).sku());
        assertEquals("MOUSE-001", result.getContent().get(1).sku());
        verify(productRepository).findAll(pageable);
    }

}