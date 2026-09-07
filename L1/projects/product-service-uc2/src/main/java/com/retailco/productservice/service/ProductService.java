package com.retailco.productservice.service;

import com.retailco.productservice.dto.ProductPageResponse;
import com.retailco.productservice.dto.ProductRequest;
import com.retailco.productservice.dto.ProductResponse;

public interface ProductService {

    ProductPageResponse listProducts(int page, int size, String category);

    ProductResponse getProductById(String id);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(String id, ProductRequest request);

    ProductPageResponse searchProducts(String query, int page, int size);
}
