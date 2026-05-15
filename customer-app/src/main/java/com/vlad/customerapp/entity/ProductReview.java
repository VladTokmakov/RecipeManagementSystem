package com.vlad.customerapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(name = "c_product_id")
    private int productId;

    @Column(name = "c_rating")
    private int rating;

    @Column(name = "c_review", length = 1000)
    private String review;
}