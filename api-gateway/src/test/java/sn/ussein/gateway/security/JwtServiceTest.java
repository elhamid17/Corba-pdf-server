package sn.ussein.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sn.ussein.gateway.config.JwtProperties;
import sn.ussein.gateway.model.Role;
import sn.ussein.gateway.model.User;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-at-least-32-characters-long");
        props.setExpirationHours(24);
        jwtService = new JwtService(props);
    }

    @Test
    void issueAndParse_roundTrip_restoresUserClaims() {
        User user = new User("alice@example.com", "alice", "hash");
        user.setId("user-123");
        user.setRoles(Set.of(Role.USER, Role.ADMIN));

        String token = jwtService.issueToken(user);
        JwtService.ParsedToken parsed = jwtService.parse(token);

        assertNotNull(parsed);
        assertEquals("user-123", parsed.userId());
        assertEquals("alice", parsed.username());
        assertEquals("alice@example.com", parsed.email());
        assertTrue(parsed.roles().contains(Role.USER));
        assertTrue(parsed.roles().contains(Role.ADMIN));
    }

    @Test
    void parse_invalidToken_returnsNull() {
        assertNull(jwtService.parse("not.a.valid.jwt"));
        assertNull(jwtService.parse(""));
    }

    @Test
    void constructor_shortSecret_throwsIllegalStateException() {
        JwtProperties props = new JwtProperties();
        props.setSecret("too-short");
        props.setExpirationHours(1);

        assertThrows(IllegalStateException.class, () -> new JwtService(props));
    }
}
