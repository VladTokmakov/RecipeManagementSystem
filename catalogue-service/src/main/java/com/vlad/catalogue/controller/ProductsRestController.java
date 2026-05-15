package com.vlad.catalogue.controller;

import com.vlad.catalogue.controller.payload.NewProductPayload;
import com.vlad.catalogue.entity.Product;
import com.vlad.catalogue.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("catalogue-api/products")
public class ProductsRestController {

    private final ProductService productService;


    @GetMapping
    public Iterable<Product> findProducts(@RequestParam(name = "filter", required = false) String filter,
                                         @RequestParam(name = "detailsFilter", required = false) String detailsFilter) {
        boolean hasFilter = filter != null && !filter.isBlank();
        boolean hasDetailsFilter = detailsFilter != null && !detailsFilter.isBlank();

        if (hasFilter && hasDetailsFilter) {
            var byTitle = this.productService.findAllProducts(filter);
            var byDetails = this.productService.findAllProductsByDetails(detailsFilter);
            var byTitleList = java.util.stream.StreamSupport.stream(byTitle.spliterator(), false).toList();
            var byDetailsList = java.util.stream.StreamSupport.stream(byDetails.spliterator(), false).toList();
            return byTitleList.stream()
                    .filter(product -> byDetailsList.stream().anyMatch(p -> p.getId().equals(product.getId())))
                    .toList();
        } else if (hasDetailsFilter) {
            return this.productService.findAllProductsByDetails(detailsFilter);
        } else if (hasFilter) {
            return this.productService.findAllProducts(filter);
        } else {
            return this.productService.findAllProducts(null);
        }
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody NewProductPayload payload,
                                           BindingResult bindingResult,
                                           UriComponentsBuilder uriComponentsBuilder)
            throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            } else {
                throw new BindException(bindingResult);
            }
        } else {
            Product product = this.productService.createProduct(payload.title(), payload.details());
            return ResponseEntity
                    .created(uriComponentsBuilder
                            .replacePath("/catalogue-api/products/{productId}")
                            .build(Map.of("productId", product.getId())))
                    .body(product);
        }
    }
}