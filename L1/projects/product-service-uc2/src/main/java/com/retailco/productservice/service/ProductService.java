package com.retailco.productservice.service;

import com.retailco.productservice.dto.ProductPageResponse;
import com.retailco.productservice.dto.ProductRequest;
import com.retailco.productservice.dto.ProductResponse;

/**
 * This interface just lists WHAT the product service can do (list, get,
 * create, update, search) — not HOW it does it. {@code ProductController}
 * depends only on this interface, never on {@code ProductServiceImpl}
 * directly. That makes it easy to swap in a different implementation
 * later (for example, a fake one used only in tests) without touching the
 * controller at all.
 */
public interface ProductService {

    ProductPageResponse listProducts(int page, int size, String category);

    ProductResponse getProductById(String id);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(String id, ProductRequest request);

    ProductPageResponse searchProducts(String query, int page, int size);
}
