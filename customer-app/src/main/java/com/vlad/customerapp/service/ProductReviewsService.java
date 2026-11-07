package com.vlad.customerapp.service;

import com.vlad.customerapp.entity.ProductReview;

import java.util.List;

public interface ProductReviewsService {

    ProductReview createProductReview(int productId, String userId, int rating, String review);

    List<ProductReview> findProductReviewsByProduct(int productId);

    void deleteProductReview(int productId, String userId);
}
