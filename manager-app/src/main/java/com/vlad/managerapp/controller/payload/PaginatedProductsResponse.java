package com.vlad.managerapp.controller.payload;

import com.vlad.managerapp.entity.Product;
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
