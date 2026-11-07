package com.vlad.customerapp;

import com.vlad.customerapp.repository.FavouriteProductRepository;
import com.vlad.customerapp.repository.ProductReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class CustomerAppApplicationTests {

    @MockBean
    private FavouriteProductRepository favouriteProductRepository;

    @MockBean
    private ProductReviewRepository productReviewRepository;

    @Test
    void contextLoads() {
    }

}
