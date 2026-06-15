package com.shopflow.product.repository;

import com.shopflow.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findAllByActiveTrue(Pageable pageable);

    Page<Product> findAllByCategoryIdAndActiveTrue(UUID categoryId, Pageable pageable);

    Optional<Product> findByIdAndActiveTrue(UUID id);

    boolean existsBySku(String sku);

    // Full-text search on name and description
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Product> search(@Param("q") String query, Pageable pageable);
}
