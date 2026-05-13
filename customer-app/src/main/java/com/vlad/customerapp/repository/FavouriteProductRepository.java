package com.vlad.customerapp.repository;

import com.vlad.customerapp.entity.FavouriteProduct;

import java.util.List;
import java.util.Optional;

public interface FavouriteProductRepository {

    FavouriteProduct save(FavouriteProduct favouriteProduct);

    void deleteByProductId(int productId);

    Optional<FavouriteProduct> findByProductId(int productId);

    List<FavouriteProduct> findAll();
}
