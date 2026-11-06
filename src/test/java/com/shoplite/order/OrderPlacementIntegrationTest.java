package com.shoplite.order;

import com.shoplite.cart.CartService;
import com.shoplite.catalog.Product;
import com.shoplite.catalog.ProductForm;
import com.shoplite.common.exception.OutOfStockException;
import com.shoplite.support.IntegrationTestSupport;
import com.shoplite.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.shoplite.support.TestData.assertMoney;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Component test: cart, order and catalog services working together on a real database.
 * Transactions are not rolled back by the test, so commit and rollback behave as in production (NFR-04).
 */
class OrderPlacementIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;

    private static CheckoutForm checkout() {
        CheckoutForm form = new CheckoutForm();
        form.setDeliveryAddress("Kyiv, 37 Beresteiskyi Ave");
        form.setPhone("+380671234567");
        return form;
    }

    @Test
    @DisplayName("FR-07, FR-08: the order is stored, stock goes down and the cart is emptied")
    void placedOrderIsStoredAndStockIsTaken() {
        User buyer = newCustomer();
        Product product = newProduct("150.00", 5);
        cartService.addItem(buyer.getId(), product.getId(), 2);

        Order order = orderService.placeOrder(buyer.getId(), checkout());

        Order stored = orderRepository.findWithItemsById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.NEW, stored.getStatus());
        assertEquals(1, stored.getItems().size());
        assertMoney("300.00", stored.getTotal());
        assertEquals(3, stockOf(product.getId()));
        assertTrue(cartService.getCart(buyer.getId()).isEmpty());
    }

    @Test
    @DisplayName("NFR-04: if one item is out of stock, nothing changes in the database")
    void outOfStockRollsBackTheWholeOrder() {
        User buyer = newCustomer();
        Product plenty = newProduct("100.00", 5);
        Product scarce = newProduct("200.00", 2);
        cartService.addItem(buyer.getId(), plenty.getId(), 2);
        cartService.addItem(buyer.getId(), scarce.getId(), 2);
        // someone else bought the last items meanwhile: the admin lowers the stock
        ProductForm form = ProductForm.from(adminCatalog.getProduct(scarce.getId()));
        form.setStock(1);
        adminCatalog.updateProduct(scarce.getId(), form);

        assertThrows(OutOfStockException.class, () -> orderService.placeOrder(buyer.getId(), checkout()));

        assertEquals(5, stockOf(plenty.getId()), "stock of the first product must be rolled back");
        assertEquals(1, stockOf(scarce.getId()));
        assertTrue(orderService.findByUser(buyer.getId()).isEmpty());
        assertEquals(2, cartService.getCart(buyer.getId()).getItems().size(), "the cart is kept");
    }

    @Test
    @DisplayName("FR-10: cancelling returns the items to stock in the database")
    void cancellationIsPersisted() {
        User buyer = newCustomer();
        Product product = newProduct("80.00", 4);
        cartService.addItem(buyer.getId(), product.getId(), 3);
        Order order = orderService.placeOrder(buyer.getId(), checkout());
        assertEquals(1, stockOf(product.getId()));

        orderService.cancel(order.getId(), buyer.getId());

        assertEquals(4, stockOf(product.getId()));
        assertEquals(OrderStatus.CANCELLED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }
}
