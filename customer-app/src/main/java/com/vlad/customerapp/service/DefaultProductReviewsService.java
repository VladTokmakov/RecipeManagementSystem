package com.vlad.customerapp.service;

import com.vlad.customerapp.entity.ProductReview;
import com.vlad.customerapp.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultProductReviewsService implements ProductReviewsService {

    private final ProductReviewRepository productReviewRepository;

    @Override
    public ProductReview createProductReview(int productId, int rating, String review) {
        return this.productReviewRepository.save(
                new ProductReview(UUID.randomUUID(), productId, rating, review));
    }

    @Override
    public List<ProductReview> findProductReviewsByProduct(int productId) {
        return this.productReviewRepository.findAllByProductId(productId);
    }
}
