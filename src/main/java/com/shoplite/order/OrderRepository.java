package com.shoplite.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Orders with a given status for the admin panel, customer loaded for display (FR-14). */
    @EntityGraph(attributePaths = "user")
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @EntityGraph(attributePaths = "user")
    List<Order> findAllByOrderByCreatedAtDesc();

    /** Loads an order with its items, products and customer for the details page. */
    @EntityGraph(attributePaths = {"items", "items.product", "user"})
    Optional<Order> findWithItemsById(Long id);

    long countByStatus(OrderStatus status);

    /** Number of non-cancelled orders in a period (FR-15). */
    @Query("""
            select count(o) from ShopOrder o
            where o.createdAt >= :from and o.createdAt < :to and o.status <> :excluded
            """)
    long countInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                       @Param("excluded") OrderStatus excluded);

    /** Revenue of non-cancelled orders in a period (FR-15). */
    @Query("""
            select coalesce(sum(o.total), 0) from ShopOrder o
            where o.createdAt >= :from and o.createdAt < :to and o.status <> :excluded
            """)
    BigDecimal revenueInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                               @Param("excluded") OrderStatus excluded);
}
