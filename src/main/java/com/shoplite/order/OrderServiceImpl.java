package com.shoplite.order;

import com.shoplite.cart.Cart;
import com.shoplite.cart.CartItem;
import com.shoplite.cart.CartService;
import com.shoplite.catalog.Product;
import com.shoplite.catalog.ProductRepository;
import com.shoplite.common.exception.BusinessException;
import com.shoplite.common.exception.EmptyCartException;
import com.shoplite.common.exception.NotFoundException;
import com.shoplite.common.exception.OutOfStockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository,
                            CartService cartService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
    }

    /**
     * Follows the "Place order" sequence diagram: products are read with a row lock,
     * and if any product is out of stock the whole transaction is rolled back.
     */
    @Override
    @Transactional
    public Order placeOrder(Long userId, CheckoutForm form) {
        Cart cart = cartService.getCart(userId);
        if (cart.isEmpty()) {
            throw new EmptyCartException();
        }
        Order order = new Order(cart.getUser(), form.getDeliveryAddress().trim(), form.getPhone().trim());
        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> new OutOfStockException(item.getProduct().getName()));
            product.decreaseStock(item.getQuantity());
            order.addItem(new OrderItem(product, item.getQuantity()));
        }
        Order saved = orderRepository.save(order);
        cartService.clear(userId);
        return saved;
    }

    @Override
    @Transactional
    public void cancel(Long orderId, Long userId) {
        Order order = getOrder(orderId);
        if (!order.getUser().getId().equals(userId)) {
            throw new NotFoundException("Order not found");
        }
        if (order.getStatus() != OrderStatus.NEW) {
            throw new BusinessException("Only a new order can be cancelled");
        }
        order.changeStatus(OrderStatus.CANCELLED);
        returnToStock(order);
    }

    @Override
    @Transactional
    public void changeStatus(Long orderId, OrderStatus status) {
        Order order = getOrder(orderId);
        order.changeStatus(status);
        if (status == OrderStatus.CANCELLED) {
            returnToStock(order);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getForUser(Long orderId, Long userId) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new NotFoundException("Order not found");
        }
        return order;
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private void returnToStock(Order order) {
        for (OrderItem item : order.getItems()) {
            item.getProduct().increaseStock(item.getQuantity());
        }
    }
}
