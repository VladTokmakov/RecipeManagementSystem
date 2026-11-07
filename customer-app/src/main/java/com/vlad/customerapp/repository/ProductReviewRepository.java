package com.vlad.customerapp.repository;

import com.vlad.customerapp.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    List<ProductReview> findAllByProductIdOrderByCreatedAtDesc(int productId);

    Optional<ProductReview> findByProductIdAndUserId(int productId, String userId);

    @Transactional
    void deleteByProductIdAndUserId(int productId, String userId);
}
