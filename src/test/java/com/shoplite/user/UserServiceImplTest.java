package com.shoplite.user;

import com.shoplite.common.exception.EmailAlreadyUsedException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserService service = new UserServiceImpl(repository, encoder);

    private RegistrationForm form(String email) {
        RegistrationForm f = new RegistrationForm();
        f.setEmail(email);
        f.setPassword("secret123");
        f.setFullName("Oksana");
        return f;
    }

    @Test
    void registerHashesPasswordAndAssignsCustomerRole() {
        when(repository.existsByEmailIgnoreCase("oksana@example.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = service.register(form(" Oksana@Example.com "));

        assertEquals("oksana@example.com", user.getEmail());
        assertEquals(Role.CUSTOMER, user.getRole());
        assertNotEquals("secret123", user.getPasswordHash());
        assertTrue(encoder.matches("secret123", user.getPasswordHash()));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(repository.existsByEmailIgnoreCase("oksana@example.com")).thenReturn(true);
        assertThrows(EmailAlreadyUsedException.class, () -> service.register(form("oksana@example.com")));
        verify(repository, never()).save(any());
    }
}
