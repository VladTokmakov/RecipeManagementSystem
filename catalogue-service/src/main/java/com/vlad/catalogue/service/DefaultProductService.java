package com.vlad.catalogue.service;

import com.vlad.catalogue.entity.Product;
import com.vlad.catalogue.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultProductService implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public Iterable<Product> findAllProducts(String filter) {
        log.debug("Finding all products with filter: {}", filter);
        if (filter != null && !filter.isBlank()) {
            return this.productRepository.findAllByTitleLikeIgnoreCase("%" + filter + "%");
        } else {
            return this.productRepository.findAll();
        }
    }

    @Override
    public Iterable<Product> findAllProductsByDetails(String filter) {
        log.debug("Finding all products by details with filter: {}", filter);
        if (filter != null && !filter.isBlank()) {
            return this.productRepository.findAllByDetailsLikeIgnoreCase("%" + filter + "%");
        } else {
            return this.productRepository.findAll();
        }
    }

    @Override
    public Page<Product> findAllProducts(String filter, String detailsFilter, Pageable pageable) {
        log.info("Finding products - filter: {}, detailsFilter: {}, page: {}, size: {}", filter, detailsFilter, pageable.getPageNumber(), pageable.getPageSize());
        boolean hasFilter = filter != null && !filter.isBlank();
        boolean hasDetailsFilter = detailsFilter != null && !detailsFilter.isBlank();

        if (hasFilter && hasDetailsFilter) {
            var byTitle = this.productRepository.findAllByTitleLikeIgnoreCase("%" + filter + "%");
            var byDetails = this.productRepository.findAllByDetailsLikeIgnoreCase("%" + detailsFilter + "%");
            var byTitleList = java.util.stream.StreamSupport.stream(byTitle.spliterator(), false).toList();
            var byDetailsList = java.util.stream.StreamSupport.stream(byDetails.spliterator(), false).toList();
            var filtered = byTitleList.stream()
                    .filter(product -> byDetailsList.stream().anyMatch(p -> p.getId().equals(product.getId())))
                    .toList();

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filtered.size());
            if (start >= filtered.size()) {
                return Page.empty(pageable);
            }
            return new org.springframework.data.domain.PageImpl<>(
                filtered.subList(start, end),
                pageable,
                filtered.size()
            );
        } else if (hasDetailsFilter) {
            return this.productRepository.findAllByDetailsLikeIgnoreCase("%" + detailsFilter + "%", pageable);
        } else if (hasFilter) {
            return this.productRepository.findAllByTitleLikeIgnoreCase("%" + filter + "%", pageable);
        } else {
            return this.productRepository.findAll(pageable);
        }
    }

    @Override
    @Transactional
    public Product createProduct(String title, String details) {
        log.info("Creating product - title: {}", title);
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
        log.info("Updating product - id: {}, title: {}", id, title);
        this.productRepository.findById(id)
                .ifPresentOrElse(product -> {
                    product.setTitle(title);
                    product.setDetails(details);
                }, () -> {
                    throw new NoSuchElementException();
                });
    }

    @Override
    @Transactional
    public void deleteProduct(Integer id) {
        log.info("Deleting product with id: {}", id);
        this.productRepository.deleteById(id);
    }
}