package com.retailco.productservice.controller;

import com.retailco.productservice.dto.ProductPageResponse;
import com.retailco.productservice.dto.ProductRequest;
import com.retailco.productservice.dto.ProductResponse;
import com.retailco.productservice.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// CONCEPT: Controller -- the entry point for HTTP requests
// (GET/POST/PUT/etc.).
// PURPOSE: Exposes the product API (list, create, get by id, update,
// search). Each method just reads the request and calls ProductService --
// no business logic lives here, only HTTP handling.
// WHY: keeps HTTP concerns (status codes, request params) separate from
// business rules (which live in ProductServiceImpl).
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ProductPageResponse> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(productService.listProducts(page, size, category));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable String id,
                                                           @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @GetMapping("/search")
    public ResponseEntity<ProductPageResponse> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.searchProducts(q, page, size));
    }
}
