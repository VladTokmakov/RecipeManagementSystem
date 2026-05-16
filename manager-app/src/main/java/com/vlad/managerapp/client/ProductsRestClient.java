package com.vlad.managerapp.client;

import com.vlad.managerapp.controller.payload.PaginatedProductsResponse;
import com.vlad.managerapp.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductsRestClient {

    List<Product> findAllProducts(String filter, String detailsFilter);

    PaginatedProductsResponse findAllProductsPaginated(String filter, String detailsFilter, int page, int size);

    Product createProduct(String title, String details);

    Optional<Product> findProduct(int productId);

    void updateProduct(int productId, String title, String details);

    void deleteProduct(int productId);
}