package com.vlad.customerapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "c_product_id", nullable = false)
    private int productId;

    @Column(name = "c_user_id", nullable = false)
    private String userId;
}
