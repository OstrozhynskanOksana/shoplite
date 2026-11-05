package com.shoplite.cart;

import com.shoplite.catalog.CatalogService;
import com.shoplite.catalog.Product;
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

    /** Returns the user's cart, creating it on first access. */
    @Override
    public Cart getCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> cartRepository.save(
                new Cart(userRepository.findById(userId)
                        .orElseThrow(() -> new NotFoundException("User not found")))));
    }

    @Override
    public void addItem(Long userId, Long productId, int quantity) {
        Product product = catalogService.getActiveProduct(productId);
        getCart(userId).addItem(product, quantity);
    }

    @Override
    public void changeQuantity(Long userId, Long productId, int quantity) {
        getCart(userId).changeQuantity(productId, quantity);
    }

    @Override
    public void removeItem(Long userId, Long productId) {
        getCart(userId).removeItem(productId);
    }

    @Override
    public void clear(Long userId) {
        getCart(userId).clear();
    }
}
