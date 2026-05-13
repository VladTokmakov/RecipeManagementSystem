package com.vlad.customerapp.client;

import com.vlad.customerapp.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class RestClientProductsClient implements ProductsClient {

    private static final ParameterizedTypeReference<List<Product>> PRODUCTS_TYPE_REFERENCE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    @Override
    public List<Product> findAllProducts(String filter) {
        return this.restClient
                .get()
                .uri("/catalogue-api/products?filter={filter}", filter)
                .retrieve()
                .body(PRODUCTS_TYPE_REFERENCE);
    }

    @Override
    public Optional<Product> findProduct(int id) {
        try {
            return Optional.ofNullable(this.restClient.get()
                    .uri("/catalogue-api/products/{productId}", id)
                    .retrieve()
                    .body(Product.class));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        }
    }
}