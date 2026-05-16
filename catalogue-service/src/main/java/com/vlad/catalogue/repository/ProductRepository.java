package com.vlad.catalogue.repository;

import com.vlad.catalogue.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends CrudRepository<Product, Integer> {

    Iterable<Product> findAllByTitleLikeIgnoreCase(String filter);

    Iterable<Product> findAllByDetailsLikeIgnoreCase(String filter);

    Page<Product> findAllByTitleLikeIgnoreCase(String filter, Pageable pageable);

    Page<Product> findAllByDetailsLikeIgnoreCase(String filter, Pageable pageable);

    Page<Product> findAll(Pageable pageable);
}