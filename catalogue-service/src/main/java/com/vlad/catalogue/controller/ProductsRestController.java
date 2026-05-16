package com.vlad.catalogue.controller;

import com.vlad.catalogue.controller.payload.NewProductPayload;
import com.vlad.catalogue.controller.payload.PaginatedProductsResponse;
import com.vlad.catalogue.entity.Product;
import com.vlad.catalogue.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("catalogue-api/products")
@Tag(name = "Products", description = "API для управления рецептами")
public class ProductsRestController {

    private final ProductService productService;

    private static final int DEFAULT_PAGE_SIZE = 10;

    @GetMapping
    @Operation(summary = "Получить список рецептов", description = "Возвращает пагинированный список рецептов с возможностью фильтрации")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список рецептов успешно получен")
    })
    public PaginatedProductsResponse findProducts(
            @Parameter(description = "Фильтр по названию рецепта") @RequestParam(name = "filter", required = false) String filter,
            @Parameter(description = "Фильтр по описанию рецепта") @RequestParam(name = "detailsFilter", required = false) String detailsFilter,
            @Parameter(description = "Номер страницы (начинается с 0)") @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @Parameter(description = "Количество элементов на странице") @RequestParam(name = "size", required = false, defaultValue = "10") int size) {
        
        log.info("GET /catalogue-api/products - filter: {}, detailsFilter: {}, page: {}, size: {}", filter, detailsFilter, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = this.productService.findAllProducts(filter, detailsFilter, pageable);
        
        PaginatedProductsResponse response = new PaginatedProductsResponse(
            productPage.getContent(),
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements(),
            productPage.getTotalPages()
        );
        
        log.info("Returning {} products, total pages: {}", response.getContent().size(), response.getTotalPages());
        return response;
    }

    @PostMapping
    @Operation(summary = "Создать новый рецепт", description = "Создает новый рецепт с указанным названием и описанием")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Рецепт успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    })
    public ResponseEntity<?> createProduct(@Valid @RequestBody NewProductPayload payload,
                                           BindingResult bindingResult,
                                           UriComponentsBuilder uriComponentsBuilder)
            throws BindException {
        log.info("POST /catalogue-api/products - title: {}", payload.title());
        
        if (bindingResult.hasErrors()) {
            log.warn("Validation errors: {}", bindingResult.getAllErrors());
            if (bindingResult instanceof BindException exception) {
                throw exception;
            } else {
                throw new BindException(bindingResult);
            }
        } else {
            Product product = this.productService.createProduct(payload.title(), payload.details());
            log.info("Product created with id: {}", product.getId());
            return ResponseEntity
                    .created(uriComponentsBuilder
                            .replacePath("/catalogue-api/products/{productId}")
                            .build(Map.of("productId", product.getId())))
                    .body(product);
        }
    }
}