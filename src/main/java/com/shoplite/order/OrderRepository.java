package com.shoplite.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<Order> findAllByOrderByCreatedAtDesc();

    /** Loads an order with its items, products and customer for the details page. */
    @EntityGraph(attributePaths = {"items", "items.product", "user"})
    Optional<Order> findWithItemsById(Long id);
}
