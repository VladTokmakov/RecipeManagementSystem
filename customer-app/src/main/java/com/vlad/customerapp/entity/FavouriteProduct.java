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
@Table(schema = "customer_app", name = "t_favourite_product")
public class FavouriteProduct {

    @Id
    private UUID id;

    @Column(name = "c_product_id")
    private int productId;

    @Column(name = "c_user_id")
    private String userId;
}