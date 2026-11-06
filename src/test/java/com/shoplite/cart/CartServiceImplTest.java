package com.shoplite.cart;

import com.shoplite.catalog.CatalogService;
import com.shoplite.catalog.Product;
import com.shoplite.common.exception.BusinessException;
import com.shoplite.common.exception.NotFoundException;
import com.shoplite.user.User;
import com.shoplite.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.shoplite.support.TestData.assertMoney;
import static com.shoplite.support.TestData.customer;
import static com.shoplite.support.TestData.product;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for the cart service (FR-06, FR-08); repositories and the catalog are mocked. */
class CartServiceImplTest {

    private final CartRepository cartRepository = mock(CartRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CatalogService catalogService = mock(CatalogService.class);
    private final CartService service = new CartServiceImpl(cartRepository, userRepository, catalogService);

    private Cart cart;
    private Product kettle;

    @BeforeEach
    void setUp() {
        User buyer = customer(1L, "buyer@example.com");
        cart = new Cart(buyer);
        kettle = product(10L, "Kettle", "899.00", 4);
        when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
        when(catalogService.getActiveProduct(10L)).thenReturn(kettle);
    }

    @Test
    @DisplayName("An empty cart is created on first access")
    void getCartCreatesEmptyCartOnFirstAccess() {
        User newcomer = customer(2L, "new@example.com");
        when(cartRepository.findWithItemsByUserId(2L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(newcomer));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart created = service.getCart(2L);

        assertTrue(created.isEmpty());
        assertSame(newcomer, created.getUser());
        verify(cartRepository).save(created);
    }

    @Test
    void getCartForUnknownUserThrowsNotFound() {
        when(cartRepository.findWithItemsByUserId(99L)).thenReturn(Optional.empty());
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getCart(99L));
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItemPutsProductIntoCartAndCountsTotal() {
        service.addItem(1L, 10L, 2);

        assertEquals(2, cart.quantityOf(10L));
        assertMoney("1798.00", cart.getTotal());
    }

    @Test
    void addingTheSameProductAgainIncreasesQuantity() {
        service.addItem(1L, 10L, 1);
        service.addItem(1L, 10L, 2);

        assertEquals(1, cart.getItems().size());
        assertEquals(3, cart.quantityOf(10L));
    }

    @Test
    @DisplayName("FR-08: cannot add more items than are in stock")
    void addItemRejectsMoreThanInStock() {
        service.addItem(1L, 10L, 3);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.addItem(1L, 10L, 2));

        assertEquals("Only 4 pcs of \"Kettle\" left in stock", ex.getMessage());
        assertEquals(3, cart.quantityOf(10L), "the cart must not change after a rejected request");
    }

    @Test
    void hiddenOrMissingProductCannotBeAdded() {
        when(catalogService.getActiveProduct(11L)).thenThrow(new NotFoundException("Product not found"));

        assertThrows(NotFoundException.class, () -> service.addItem(1L, 11L, 1));
        assertTrue(cart.isEmpty());
    }

    @Test
    void changeQuantityIsCheckedAgainstStock() {
        service.addItem(1L, 10L, 1);

        assertThrows(BusinessException.class, () -> service.changeQuantity(1L, 10L, 5));
        assertEquals(1, cart.quantityOf(10L));

        service.changeQuantity(1L, 10L, 4);
        assertEquals(4, cart.quantityOf(10L));
    }

    @Test
    void changeQuantityToZeroRemovesTheItem() {
        service.addItem(1L, 10L, 2);

        service.changeQuantity(1L, 10L, 0);

        assertTrue(cart.isEmpty());
    }

    @Test
    void changeQuantityOfProductThatIsNotInCartDoesNothing() {
        service.changeQuantity(1L, 10L, 2);

        assertTrue(cart.isEmpty());
    }

    @Test
    void removeItemAndClear() {
        Product mop = product(20L, "Mop", "980.00", 5);
        when(catalogService.getActiveProduct(20L)).thenReturn(mop);
        service.addItem(1L, 10L, 1);
        service.addItem(1L, 20L, 1);

        service.removeItem(1L, 10L);
        assertEquals(0, cart.quantityOf(10L));
        assertEquals(1, cart.quantityOf(20L));

        service.clear(1L);
        assertTrue(cart.isEmpty());
        assertMoney("0", cart.getTotal());
    }
}
