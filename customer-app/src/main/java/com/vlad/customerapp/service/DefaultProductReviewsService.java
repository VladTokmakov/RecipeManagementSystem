package com.vlad.customerapp.service;

import com.vlad.customerapp.entity.ProductReview;
import com.vlad.customerapp.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultProductReviewsService implements ProductReviewsService {

    private final ProductReviewRepository productReviewRepository;

    @Override
    @Transactional
    public ProductReview createProductReview(int productId, int rating, String review) {
        ProductReview productReview = new ProductReview();
        productReview.setProductId(productId);
        productReview.setRating(rating);
        productReview.setReview(review);
        return this.productReviewRepository.save(productReview);
    }

    @Override
    public List<ProductReview> findProductReviewsByProduct(int productId) {
        return this.productReviewRepository.findAllByProductId(productId);
    }
}
