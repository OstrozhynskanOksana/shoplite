package com.shoplite.order;

import com.shoplite.common.exception.IllegalStatusTransitionException;
import com.shoplite.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(User user, String deliveryAddress, String phone) {
        this.user = user;
        this.deliveryAddress = deliveryAddress;
        this.phone = phone;
        this.status = OrderStatus.NEW;
        this.createdAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        item.attachTo(this);
        items.add(item);
        total = total.add(item.getSubtotal());
    }

    /** Changes the status if the transition is allowed (FR-10, FR-14). */
    public void changeStatus(OrderStatus next) {
        if (!status.canChangeTo(next)) {
            throw new IllegalStatusTransitionException(status, next);
        }
        status = next;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return status; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getPhone() { return phone; }
    public BigDecimal getTotal() { return total; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
}
