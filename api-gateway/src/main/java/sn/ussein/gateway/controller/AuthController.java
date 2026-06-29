package sn.ussein.gateway.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import sn.ussein.gateway.dto.AuthResponse;
import sn.ussein.gateway.dto.LoginRequest;
import sn.ussein.gateway.dto.MeResponse;
import sn.ussein.gateway.dto.RegisterRequest;
import sn.ussein.gateway.model.User;
import sn.ussein.gateway.repository.UserRepository;
import sn.ussein.gateway.security.AuthenticatedPrincipal;
import sn.ussein.gateway.service.AuthService;
import sn.ussein.gateway.web.ApiPaths;
import sn.ussein.gateway.web.AuthException;

import java.util.stream.Collectors;

@Tag(name = "Authentification",
     description = "Inscription, connexion (JWT) et profil de l'utilisateur courant.")
@RestController
@RequestMapping(ApiPaths.AUTH)
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new AuthException("Non authentifie.");
        }
        User user = users.findById(principal.userId())
            .orElseThrow(() -> new AuthException("Utilisateur introuvable."));

        return ResponseEntity.ok(new MeResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
            user.getStorageBytesUsed(),
            user.getCreatedAt(),
            user.getLastLoginAt()
        ));
    }
}
