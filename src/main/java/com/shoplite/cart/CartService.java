package com.shoplite.cart;

/** Customer cart operations (FR-06). Cart pages are planned for Sprint 2. */
public interface CartService {

    Cart getCart(Long userId);

    void addItem(Long userId, Long productId, int quantity);

    void changeQuantity(Long userId, Long productId, int quantity);

    void removeItem(Long userId, Long productId);

    void clear(Long userId);
}
