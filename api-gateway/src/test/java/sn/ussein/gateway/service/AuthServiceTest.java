package sn.ussein.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import sn.ussein.gateway.config.JwtProperties;
import sn.ussein.gateway.dto.LoginRequest;
import sn.ussein.gateway.dto.RegisterRequest;
import sn.ussein.gateway.model.User;
import sn.ussein.gateway.repository.UserRepository;
import sn.ussein.gateway.security.JwtService;
import sn.ussein.gateway.web.AuthException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository users;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProps = new JwtProperties();
        jwtProps.setSecret("test-secret-key-at-least-32-characters-long");
        jwtProps.setExpirationHours(24);

        JwtService jwtService = new JwtService(jwtProps);
        authService = new AuthService(users, new BCryptPasswordEncoder(), jwtService, jwtProps);
    }

    @Test
    void register_success_returnsToken() {
        when(users.existsByEmail("bob@example.com")).thenReturn(false);
        when(users.existsByUsernameIgnoreCase("bob")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("id-bob");
            return u;
        });

        var response = authService.register(new RegisterRequest("bob@example.com", "bob", "secret123"));

        assertNotNull(response.token());
        assertEquals("bob", response.user().username());
        assertEquals("bob@example.com", response.user().email());
    }

    @Test
    void register_duplicateEmail_throwsAuthException() {
        when(users.existsByEmail("dup@example.com")).thenReturn(true);

        assertThrows(AuthException.class,
            () -> authService.register(new RegisterRequest("dup@example.com", "dup", "pwd")));
    }

    @Test
    void login_withUsername_success() {
        User stored = new User("carol@example.com", "Carol", new BCryptPasswordEncoder().encode("pass"));
        stored.setId("id-carol");
        when(users.findByUsernameIgnoreCase("carol")).thenReturn(Optional.of(stored));

        var response = authService.login(new LoginRequest("carol", "pass"));

        assertNotNull(response.token());
        verify(users).save(any(User.class));
    }

    @Test
    void login_wrongPassword_throwsAuthException() {
        User stored = new User("d@example.com", "dan", new BCryptPasswordEncoder().encode("good"));
        when(users.findByEmail("d@example.com")).thenReturn(Optional.of(stored));

        assertThrows(AuthException.class,
            () -> authService.login(new LoginRequest("d@example.com", "bad")));
    }

    @Test
    void register_normalizesEmailToLowerCase() {
        when(users.existsByEmail("eve@example.com")).thenReturn(false);
        when(users.existsByUsernameIgnoreCase("eve")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(new RegisterRequest("Eve@Example.COM", "eve", "pwd"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertEquals("eve@example.com", captor.getValue().getEmail());
    }
}
