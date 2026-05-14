package com.vlad.customerapp.repository;

import com.vlad.customerapp.entity.FavouriteProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavouriteProductRepository extends JpaRepository<FavouriteProduct, UUID> {

    @Transactional
    void deleteByProductIdAndUserId(int productId, String userId);

    Optional<FavouriteProduct> findByProductIdAndUserId(int productId, String userId);

    List<FavouriteProduct> findAllByUserId(String userId);
}
