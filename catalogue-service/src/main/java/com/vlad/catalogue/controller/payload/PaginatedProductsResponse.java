package com.vlad.catalogue.controller.payload;

import com.vlad.catalogue.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PaginatedProductsResponse {
    private List<Product> content;
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
}
