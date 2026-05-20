package sn.ussein.gateway.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sn.ussein.gateway.config.JwtProperties;
import sn.ussein.gateway.dto.AuthResponse;
import sn.ussein.gateway.dto.LoginRequest;
import sn.ussein.gateway.dto.RegisterRequest;
import sn.ussein.gateway.model.User;
import sn.ussein.gateway.repository.UserRepository;
import sn.ussein.gateway.security.JwtService;
import sn.ussein.gateway.web.AuthException;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expirationSeconds;

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProps) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.expirationSeconds = jwtProps.getExpirationHours() * 3600L;
    }

    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        String username = req.username().trim();

        if (users.existsByEmail(email)) {
            throw new AuthException("Un compte avec cet email existe deja.");
        }
        // Comparaison insensible a la casse pour eviter qu'on cree
        // "JeanDupont" et "jeandupont" comme deux comptes distincts.
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new AuthException("Ce nom d'utilisateur est deja pris.");
        }

        User user = new User(email, username, passwordEncoder.encode(req.password()));
        user = users.save(user);

        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        String id = req.identifier().trim();
        // Resolution insensible a la casse aussi bien pour l'email que le username
        // pour que l'utilisateur n'ait pas a respecter la casse exacte d'inscription.
        Optional<User> found = id.contains("@")
            ? users.findByEmail(id.toLowerCase())
            : users.findByUsernameIgnoreCase(id);

        User user = found.orElseThrow(
            () -> new AuthException("Identifiants invalides."));

        if (!user.isEnabled()) {
            throw new AuthException("Ce compte est desactive.");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new AuthException("Identifiants invalides.");
        }

        user.setLastLoginAt(Instant.now());
        users.save(user);

        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.issueToken(user);
        AuthResponse.UserSummary summary = new AuthResponse.UserSummary(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
        );
        return new AuthResponse(token, expirationSeconds, summary);
    }
}
