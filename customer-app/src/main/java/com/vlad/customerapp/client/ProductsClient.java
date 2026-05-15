package com.vlad.customerapp.client;

import com.vlad.customerapp.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductsClient {
    List<Product> findAllProducts(String filter, String detailsFilter);

    Optional<Product> findProduct(int id);
}