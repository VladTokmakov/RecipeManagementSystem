package com.vlad.customerapp.repository;

import com.vlad.customerapp.entity.ProductReview;

import java.util.List;

public interface ProductReviewRepository {

    ProductReview save(ProductReview productReview);

    List<ProductReview> findAllByProductId(int productId);
}
