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
    public FavouriteProduct addProductToFavourites(int productId) {
        return this.favouriteProductRepository.save(new FavouriteProduct(UUID.randomUUID(), productId));
    }

    @Override
    public void removeProductFromFavourites(int productId) {
        this.favouriteProductRepository.deleteByProductId(productId);
    }

    @Override
    public Optional<FavouriteProduct> findFavouriteProductByProduct(int productId) {
        return this.favouriteProductRepository.findByProductId(productId);
    }

    @Override
    public List<FavouriteProduct> findFavouriteProducts() {
        return this.favouriteProductRepository.findAll();
    }
}
