package com.vlad.customerapp.repository;

import com.vlad.customerapp.entity.ProductReview;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Repository
public class InMemoryProductReviewRepository implements ProductReviewRepository {

    private final List<ProductReview> productReviews = Collections.synchronizedList(new LinkedList<>());

    @Override
    public ProductReview save(ProductReview productReview) {
        this.productReviews.add(productReview);
        return productReview;
    }

    @Override
    public List<ProductReview> findAllByProductId(int productId) {
        return this.productReviews.stream()
                .filter(productReview -> productReview.getProductId() == productId)
                .toList();
    }
}