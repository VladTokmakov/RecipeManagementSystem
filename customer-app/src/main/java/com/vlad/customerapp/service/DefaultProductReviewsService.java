package com.vlad.customerapp.service;

import com.vlad.customerapp.entity.ProductReview;
import com.vlad.customerapp.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultProductReviewsService implements ProductReviewsService {

    private final ProductReviewRepository productReviewRepository;

    @Override
    @Transactional
    public ProductReview createProductReview(int productId, String userId, int rating, String review) {
        ProductReview productReview = this.productReviewRepository
                .findByProductIdAndUserId(productId, userId)
                .orElseGet(() -> {
                    ProductReview created = new ProductReview();
                    created.setProductId(productId);
                    created.setUserId(userId);
                    created.setCreatedAt(Instant.now());
                    return created;
                });

        productReview.setRating(rating);
        productReview.setReview(review);

        return this.productReviewRepository.save(productReview);
    }

    @Override
    public List<ProductReview> findProductReviewsByProduct(int productId) {
        return this.productReviewRepository.findAllByProductIdOrderByCreatedAtDesc(productId);
    }

    @Override
    @Transactional
    public void deleteProductReview(int productId, String userId) {
        this.productReviewRepository.deleteByProductIdAndUserId(productId, userId);
    }
}
