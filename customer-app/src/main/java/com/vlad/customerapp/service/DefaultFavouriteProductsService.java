package com.vlad.customerapp.service;

import com.vlad.customerapp.entity.FavouriteProduct;
import com.vlad.customerapp.repository.FavouriteProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultFavouriteProductsService implements FavouriteProductsService {

    private final FavouriteProductRepository favouriteProductRepository;

    @Override
    public FavouriteProduct addProductToFavourites(int productId, String userId) {
        return this.favouriteProductRepository.save(new FavouriteProduct(UUID.randomUUID(), productId, userId));
    }

    @Override
    public void removeProductFromFavourites(int productId, String userId) {
        this.favouriteProductRepository.deleteByProductIdAndUserId(productId, userId);
    }

    @Override
    public Optional<FavouriteProduct> findFavouriteProductByProduct(int productId, String userId) {
        return this.favouriteProductRepository.findByProductIdAndUserId(productId, userId);
    }

    @Override
    public List<FavouriteProduct> findFavouriteProducts(String userId) {
        return this.favouriteProductRepository.findAllByUserId(userId);
    }
}
