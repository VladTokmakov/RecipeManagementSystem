package com.vlad.customerapp.repository;

import com.vlad.customerapp.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, java.util.UUID> {

    List<ProductReview> findAllByProductId(int productId);
}