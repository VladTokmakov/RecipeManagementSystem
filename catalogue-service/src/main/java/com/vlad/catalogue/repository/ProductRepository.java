package com.vlad.catalogue.repository;

import com.vlad.catalogue.entity.Product;
import org.springframework.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends CrudRepository<Product, Integer> {

    Iterable<Product> findAllByTitleLikeIgnoreCase(String filter);

    Iterable<Product> findAllByDetailsLikeIgnoreCase(String filter);
}