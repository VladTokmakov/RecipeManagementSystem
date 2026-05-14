package com.vlad.customerapp.service;

import com.vlad.customerapp.entity.FavouriteProduct;

import java.util.List;
import java.util.Optional;

public interface FavouriteProductsService {

    FavouriteProduct addProductToFavourites(int productId, String userId);

    void removeProductFromFavourites(int productId, String userId);

    Optional<FavouriteProduct> findFavouriteProductByProduct(int productId, String userId);

    List<FavouriteProduct> findFavouriteProducts(String userId);
}
