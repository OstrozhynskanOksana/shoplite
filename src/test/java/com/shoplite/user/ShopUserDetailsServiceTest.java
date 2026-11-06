package com.shoplite.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Unit tests for the adapter between users and Spring Security (FR-02, NFR-03). */
class ShopUserDetailsServiceTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final ShopUserDetailsService service = new ShopUserDetailsService(repository);

    @Test
    void userIsLoadedByEmailWithRole() {
        User admin = new User("admin@shoplite.local", "$2a$10$hash", "Administrator", Role.ADMIN);
        when(repository.findByEmailIgnoreCase("ADMIN@shoplite.local")).thenReturn(Optional.of(admin));

        UserDetails details = service.loadUserByUsername("ADMIN@shoplite.local");

        assertEquals("admin@shoplite.local", details.getUsername());
        assertEquals("$2a$10$hash", details.getPassword());
        assertTrue(details.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals));
    }

    @Test
    void unknownEmailGivesTheSameMessageAsWrongPassword() {
        when(repository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("nobody@example.com"));
        assertEquals("Invalid email or password", ex.getMessage());
    }
}
