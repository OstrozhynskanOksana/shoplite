package com.shoplite.user;

import com.shoplite.common.exception.BusinessException;
import com.shoplite.common.exception.EmailAlreadyUsedException;
import com.shoplite.common.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for registration and profile changes (FR-01, FR-11, NFR-02). */
class UserServiceImplTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserService service = new UserServiceImpl(repository, encoder);

    private RegistrationForm form(String email) {
        RegistrationForm f = new RegistrationForm();
        f.setEmail(email);
        f.setPassword("secret123");
        f.setFullName("  Oksana ");
        return f;
    }

    private User storedUser(String password) {
        User user = new User("oksana@example.com", encoder.encode(password), "Oksana", Role.CUSTOMER);
        when(repository.findByEmailIgnoreCase("oksana@example.com")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    @DisplayName("FR-01, NFR-02: password is stored only as a BCrypt hash")
    void registerHashesPasswordAndAssignsCustomerRole() {
        when(repository.existsByEmailIgnoreCase("oksana@example.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = service.register(form(" Oksana@Example.com "));

        assertEquals("oksana@example.com", user.getEmail());
        assertEquals("Oksana", user.getFullName());
        assertEquals(Role.CUSTOMER, user.getRole());
        assertNotEquals("secret123", user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2"));
        assertTrue(encoder.matches("secret123", user.getPasswordHash()));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(repository.existsByEmailIgnoreCase("oksana@example.com")).thenReturn(true);
        assertThrows(EmailAlreadyUsedException.class, () -> service.register(form("oksana@example.com")));
        verify(repository, never()).save(any());
    }

    @Test
    void unknownEmailIsNotFound() {
        when(repository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getByEmail("nobody@example.com"));
    }

    @Test
    @DisplayName("FR-11: profile update trims the name and stores an empty phone as null")
    void updateProfileTrimsValues() {
        User user = storedUser("secret123");
        ProfileForm profile = new ProfileForm();
        profile.setFullName("  Oksana Ostrozhynska ");
        profile.setPhone(" +380 67 123 4567 ");

        service.updateProfile("oksana@example.com", profile);
        assertEquals("Oksana Ostrozhynska", user.getFullName());
        assertEquals("+380 67 123 4567", user.getPhone());

        profile.setPhone("   ");
        service.updateProfile("oksana@example.com", profile);
        assertNull(user.getPhone());
    }

    @Test
    void passwordIsNotChangedWhenCurrentPasswordIsWrong() {
        User user = storedUser("secret123");
        String oldHash = user.getPasswordHash();
        PasswordChangeForm change = new PasswordChangeForm();
        change.setCurrentPassword("wrong-password");
        change.setNewPassword("newsecret123");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changePassword("oksana@example.com", change));

        assertEquals("Current password is incorrect", ex.getMessage());
        assertEquals(oldHash, user.getPasswordHash());
    }

    @Test
    void passwordChangeStoresNewHash() {
        User user = storedUser("secret123");
        PasswordChangeForm change = new PasswordChangeForm();
        change.setCurrentPassword("secret123");
        change.setNewPassword("newsecret123");

        service.changePassword("oksana@example.com", change);

        assertTrue(encoder.matches("newsecret123", user.getPasswordHash()));
        assertFalse(encoder.matches("secret123", user.getPasswordHash()));
    }
}
