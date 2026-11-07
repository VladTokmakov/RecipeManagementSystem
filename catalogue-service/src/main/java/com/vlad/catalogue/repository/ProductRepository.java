package com.vlad.catalogue.repository;

import com.vlad.catalogue.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    Page<Product> findAll(Pageable pageable);
}
