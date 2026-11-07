package com.vlad.catalogue.service;

import com.vlad.catalogue.entity.Product;
import com.vlad.catalogue.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultProductService implements ProductService {

    private final ProductRepository productRepository;

    private static final char LIKE_ESCAPE = '\\';

    @Override
    public Page<Product> findAllProducts(String filter, String detailsFilter, Pageable pageable) {
        log.info("Finding products - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        boolean hasFilter = filter != null && !filter.isBlank();
        boolean hasDetailsFilter = detailsFilter != null && !detailsFilter.isBlank();

        Specification<Product> specification = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (hasFilter) {
                predicates.add(cb.like(cb.lower(root.get("title")),
                        "%" + escapeLike(filter.toLowerCase()) + "%", LIKE_ESCAPE));
            }
            if (hasDetailsFilter) {
                predicates.add(cb.like(cb.lower(root.get("details")),
                        "%" + escapeLike(detailsFilter.toLowerCase()) + "%", LIKE_ESCAPE));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return this.productRepository.findAll(specification, pageable);
    }

    private static String escapeLike(String value) {
        return value
                .replace(String.valueOf(LIKE_ESCAPE), String.valueOf(LIKE_ESCAPE) + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }

    @Override
    @Transactional
    public Product createProduct(String title, String details) {
        log.info("Creating product");
        Product product = this.productRepository.save(new Product(null, title, details));
        log.info("Product created with id: {}", product.getId());
        return product;
    }

    @Override
    public Optional<Product> findProduct(int productId) {
        log.debug("Finding product with id: {}", productId);
        return this.productRepository.findById(productId);
    }

    @Override
    @Transactional
    public void updateProduct(Integer id, String title, String details) {
        log.info("Updating product - id: {}", id);
        this.productRepository.findById(id)
                .ifPresentOrElse(product -> {
                    product.setTitle(title);
                    product.setDetails(details);
                }, () -> {
                    throw new NoSuchElementException("catalogue.errors.product.not_found");
                });
    }

    @Override
    @Transactional
    public void deleteProduct(Integer id) {
        log.info("Deleting product with id: {}", id);
        Product product = this.productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("catalogue.errors.product.not_found"));
        this.productRepository.delete(product);
    }
}
