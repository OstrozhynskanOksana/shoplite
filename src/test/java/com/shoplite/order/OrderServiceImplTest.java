package com.shoplite.order;

import com.shoplite.cart.Cart;
import com.shoplite.cart.CartService;
import com.shoplite.catalog.Product;
import com.shoplite.catalog.ProductRepository;
import com.shoplite.common.exception.BusinessException;
import com.shoplite.common.exception.EmptyCartException;
import com.shoplite.common.exception.IllegalStatusTransitionException;
import com.shoplite.common.exception.NotFoundException;
import com.shoplite.common.exception.OutOfStockException;
import com.shoplite.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.shoplite.support.TestData.assertMoney;
import static com.shoplite.support.TestData.customer;
import static com.shoplite.support.TestData.product;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for order placement, cancellation and status changes (FR-07 – FR-10, FR-14). */
class OrderServiceImplTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final CartService cartService = mock(CartService.class);
    private final OrderService service = new OrderServiceImpl(orderRepository, productRepository, cartService);

    private User buyer;
    private Cart cart;
    private Product kettle;
    private Product mop;

    @BeforeEach
    void setUp() {
        buyer = customer(1L, "buyer@example.com");
        cart = new Cart(buyer);
        kettle = product(10L, "Kettle", "899.00", 4);
        mop = product(20L, "Mop", "980.00", 5);
        when(cartService.getCart(1L)).thenReturn(cart);
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(kettle));
        when(productRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(mop));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static CheckoutForm form() {
        CheckoutForm form = new CheckoutForm();
        form.setDeliveryAddress("  Kyiv, 37 Beresteiskyi Ave  ");
        form.setPhone(" +380 67 123 4567 ");
        return form;
    }

    /** An order of 2 kettles that is already stored, as the repository would return it. */
    private Order storedOrder() {
        kettle.decreaseStock(2);
        Order order = new Order(buyer, "Kyiv", "+380671234567");
        order.addItem(new OrderItem(kettle, 2));
        when(orderRepository.findWithItemsById(5L)).thenReturn(Optional.of(order));
        return order;
    }

    @Test
    @DisplayName("FR-07, FR-08: order is created from the cart and stock is decreased")
    void placeOrderCreatesNewOrderAndTakesItemsFromStock() {
        cart.addItem(kettle, 1);
        cart.addItem(mop, 2);

        Order order = service.placeOrder(1L, form());

        assertEquals(OrderStatus.NEW, order.getStatus());
        assertSame(buyer, order.getUser());
        assertEquals("Kyiv, 37 Beresteiskyi Ave", order.getDeliveryAddress());
        assertEquals("+380 67 123 4567", order.getPhone());
        assertEquals(2, order.getItems().size());
        assertMoney("2859.00", order.getTotal());
        assertEquals(3, kettle.getStock());
        assertEquals(3, mop.getStock());
        verify(orderRepository).save(order);
        verify(cartService).clear(1L);
    }

    @Test
    void laterPriceChangesDoNotAffectPlacedOrders() {
        cart.addItem(kettle, 1);
        Order order = service.placeOrder(1L, form());

        kettle.setPrice(new BigDecimal("999.00"));

        assertMoney("899.00", order.getItems().get(0).getPriceAtPurchase());
        assertMoney("899.00", order.getTotal());
    }

    @Test
    void emptyCartCannotBeOrdered() {
        assertThrows(EmptyCartException.class, () -> service.placeOrder(1L, form()));

        verify(orderRepository, never()).save(any());
        verify(cartService, never()).clear(any());
    }

    @Test
    @DisplayName("NFR-04: not enough stock stops the order, nothing is saved")
    void placeOrderFailsWhenStockIsNotEnough() {
        cart.addItem(kettle, 1);
        cart.addItem(mop, 2);
        mop.setStock(1);

        assertThrows(OutOfStockException.class, () -> service.placeOrder(1L, form()));

        verify(orderRepository, never()).save(any());
        verify(cartService, never()).clear(any());
    }

    @Test
    void productHiddenAfterAddingToCartCannotBeOrdered() {
        cart.addItem(kettle, 1);
        kettle.setActive(false);

        assertThrows(OutOfStockException.class, () -> service.placeOrder(1L, form()));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-10: customer cancels a new order, items return to stock")
    void cancelNewOrderReturnsItemsToStock() {
        Order order = storedOrder();
        assertEquals(2, kettle.getStock());

        service.cancel(5L, 1L);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(4, kettle.getStock());
    }

    @Test
    void cancellingSomebodyElsesOrderLooksLikeMissingOrder() {
        Order order = storedOrder();

        assertThrows(NotFoundException.class, () -> service.cancel(5L, 2L));
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertEquals(2, kettle.getStock());
    }

    @Test
    void onlyNewOrdersCanBeCancelledByCustomer() {
        Order order = storedOrder();
        order.changeStatus(OrderStatus.CONFIRMED);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.cancel(5L, 1L));

        assertEquals("Only a new order can be cancelled", ex.getMessage());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(2, kettle.getStock());
    }

    @Test
    @DisplayName("FR-14: admin moves the order along the allowed transitions")
    void adminMovesOrderAlongAllowedTransitions() {
        Order order = storedOrder();

        service.changeStatus(5L, OrderStatus.CONFIRMED);
        service.changeStatus(5L, OrderStatus.SHIPPED);
        service.changeStatus(5L, OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(2, kettle.getStock(), "delivery does not change stock");
    }

    @Test
    void adminCannotSkipOrReverseStatuses() {
        Order order = storedOrder();

        assertThrows(IllegalStatusTransitionException.class, () -> service.changeStatus(5L, OrderStatus.DELIVERED));
        service.changeStatus(5L, OrderStatus.CONFIRMED);
        service.changeStatus(5L, OrderStatus.SHIPPED);
        assertThrows(IllegalStatusTransitionException.class, () -> service.changeStatus(5L, OrderStatus.CANCELLED));

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void cancellationByAdminReturnsItemsToStock() {
        storedOrder();

        service.changeStatus(5L, OrderStatus.CONFIRMED);
        service.changeStatus(5L, OrderStatus.CANCELLED);

        assertEquals(4, kettle.getStock());
    }

    @Test
    @DisplayName("FR-09: a customer sees only own orders")
    void getForUserHidesOtherCustomersOrders() {
        Order order = storedOrder();

        assertSame(order, service.getForUser(5L, 1L));
        assertThrows(NotFoundException.class, () -> service.getForUser(5L, 2L));
    }

    @Test
    void unknownOrderIsNotFound() {
        when(orderRepository.findWithItemsById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(404L));
        assertThrows(NotFoundException.class, () -> service.cancel(404L, 1L));
        assertThrows(NotFoundException.class, () -> service.changeStatus(404L, OrderStatus.CONFIRMED));
    }

    @Test
    void listsUseTheRightQueries() {
        Order order = storedOrder();
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.NEW)).thenReturn(List.of(order));

        assertEquals(List.of(order), service.findByUser(1L));
        assertEquals(List.of(order), service.findAll(null));
        assertEquals(List.of(order), service.findAll(OrderStatus.NEW));
        assertSame(order, service.getById(5L));
        verify(orderRepository).findAllByOrderByCreatedAtDesc();
        verify(orderRepository).findByStatusOrderByCreatedAtDesc(OrderStatus.NEW);
    }
}
