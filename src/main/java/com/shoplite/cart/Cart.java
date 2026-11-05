package com.shoplite.cart;

import com.shoplite.catalog.Product;
import com.shoplite.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Customer cart. Stored in the database, so it survives between sessions (FR-06). */
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public Cart(User user) {
        this.user = user;
    }

    /** Adds a product; if it is already in the cart, increases the quantity. */
    public void addItem(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        findItem(product.getId()).ifPresentOrElse(
                item -> item.setQuantity(item.getQuantity() + quantity),
                () -> items.add(new CartItem(this, product, quantity)));
    }

    public void changeQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            removeItem(productId);
            return;
        }
        findItem(productId).ifPresent(item -> item.setQuantity(quantity));
    }

    public void removeItem(Long productId) {
        items.removeIf(item -> item.getProduct().getId().equals(productId));
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<CartItem> findItem(Long productId) {
        return items.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public List<CartItem> getItems() { return Collections.unmodifiableList(items); }
}
