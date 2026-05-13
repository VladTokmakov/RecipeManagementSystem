package com.vlad.customerapp.service;

import com.vlad.customerapp.entity.FavouriteProduct;

import java.util.List;
import java.util.Optional;

public interface FavouriteProductsService {

    FavouriteProduct addProductToFavourites(int productId);

    void removeProductFromFavourites(int productId);

    Optional<FavouriteProduct> findFavouriteProductByProduct(int productId);

    List<FavouriteProduct> findFavouriteProducts();
}
