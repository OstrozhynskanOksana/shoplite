package com.shoplite.catalog;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Searches active products with optional name and category filters (FR-03, FR-04).
     * An empty name means "no name filter".
     */
    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where p.active = true
              and lower(p.name) like lower(concat('%', :name, '%'))
              and (:categoryId is null or p.category.id = :categoryId)
            """)
    Page<Product> searchActive(@Param("name") String name,
                               @Param("categoryId") Long categoryId,
                               Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findByIdAndActiveTrue(Long id);

    /** Reads a product with a row lock for order placement (NFR-04). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
