package com.vlad.catalogue.service;

import com.vlad.catalogue.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    Iterable<Product> findAllProducts(String filter);

    Iterable<Product> findAllProductsByDetails(String filter);

    Page<Product> findAllProducts(String filter, String detailsFilter, Pageable pageable);

    Product createProduct(String title, String details);

    Optional<Product> findProduct(int productId);

    void updateProduct(Integer id, String title, String details);

    void deleteProduct(Integer id);
}