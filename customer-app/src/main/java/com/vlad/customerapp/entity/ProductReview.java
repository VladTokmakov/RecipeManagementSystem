package com.vlad.customerapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "customer_app", name = "t_product_review")
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "c_product_id", nullable = false)
    private int productId;

    @Column(name = "c_user_id", nullable = false)
    private String userId;

    @Column(name = "c_rating", nullable = false)
    private int rating;

    @Column(name = "c_review", length = 1000)
    private String review;

    @Column(name = "c_created_at", nullable = false)
    private Instant createdAt;
}
