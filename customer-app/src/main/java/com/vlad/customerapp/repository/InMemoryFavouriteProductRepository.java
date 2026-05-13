package com.vlad.customerapp.repository;

import com.vlad.customerapp.entity.FavouriteProduct;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryFavouriteProductRepository implements FavouriteProductRepository {

    private final List<FavouriteProduct> favouriteProducts = Collections.synchronizedList(new LinkedList<>());

    @Override
    public FavouriteProduct save(FavouriteProduct favouriteProduct) {
        this.favouriteProducts.add(favouriteProduct);
        return favouriteProduct;
    }

    @Override
    public void deleteByProductId(int productId) {
        this.favouriteProducts.removeIf(favouriteProduct -> favouriteProduct.getProductId() == productId);
    }

    @Override
    public Optional<FavouriteProduct> findByProductId(int productId) {
        return this.favouriteProducts.stream()
                .filter(favouriteProduct -> favouriteProduct.getProductId() == productId)
                .findFirst();
    }

    @Override
    public List<FavouriteProduct> findAll() {
        return List.copyOf(this.favouriteProducts);
    }
}