package com.vlad.customerapp.client;

import com.vlad.customerapp.controller.payload.PaginatedProductsResponse;
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

    private static final ParameterizedTypeReference<PaginatedProductsResponse> PAGINATED_PRODUCTS_TYPE_REFERENCE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    @Override
    public List<Product> findAllProducts(String filter, String detailsFilter) {
        return this.restClient
                .get()
                .uri("/catalogue-api/products?filter={filter}&detailsFilter={detailsFilter}", filter, detailsFilter)
                .retrieve()
                .body(PRODUCTS_TYPE_REFERENCE);
    }

    @Override
    public PaginatedProductsResponse findAllProductsPaginated(String filter, String detailsFilter, int page, int size) {
        return this.restClient
                .get()
                .uri("/catalogue-api/products?filter={filter}&detailsFilter={detailsFilter}&page={page}&size={size}", 
                     filter, detailsFilter, page, size)
                .retrieve()
                .body(PAGINATED_PRODUCTS_TYPE_REFERENCE);
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