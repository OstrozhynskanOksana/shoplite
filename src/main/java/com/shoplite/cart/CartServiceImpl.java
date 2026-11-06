package com.shoplite.cart;

import com.shoplite.catalog.CatalogService;
import com.shoplite.catalog.Product;
import com.shoplite.common.exception.BusinessException;
import com.shoplite.common.exception.NotFoundException;
import com.shoplite.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final CatalogService catalogService;

    public CartServiceImpl(CartRepository cartRepository, UserRepository userRepository,
                           CatalogService catalogService) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.catalogService = catalogService;
    }

    /** Returns the user's cart with its items, creating an empty cart on first access. */
    @Override
    public Cart getCart(Long userId) {
        return cartRepository.findWithItemsByUserId(userId).orElseGet(() -> cartRepository.save(
                new Cart(userRepository.findById(userId)
                        .orElseThrow(() -> new NotFoundException("User not found")))));
    }

    @Override
    public void addItem(Long userId, Long productId, int quantity) {
        Product product = catalogService.getActiveProduct(productId);
        Cart cart = getCart(userId);
        checkStock(product, cart.quantityOf(productId), quantity);
        cart.addItem(product, quantity);
    }

    @Override
    public void changeQuantity(Long userId, Long productId, int quantity) {
        Cart cart = getCart(userId);
        cart.getItem(productId).ifPresent(item -> {
            if (quantity > 0) {
                checkStock(item.getProduct(), 0, quantity);
            }
            cart.changeQuantity(productId, quantity);
        });
    }

    @Override
    public void removeItem(Long userId, Long productId) {
        getCart(userId).removeItem(productId);
    }

    @Override
    public void clear(Long userId) {
        getCart(userId).clear();
    }

    /**
     * Checks that {@code toAdd} more items fit into the stock when {@code alreadyInCart} are already reserved.
     * The two numbers are not added together, so a huge quantity cannot overflow int (SCRUM-37).
     */
    private static void checkStock(Product product, int alreadyInCart, int toAdd) {
        if (toAdd > product.getStock() - alreadyInCart) {
            throw new BusinessException("Only " + product.getStock() + " pcs of \"" + product.getName()
                    + "\" left in stock");
        }
    }
}
