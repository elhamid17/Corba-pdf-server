package sn.ussein.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sn.ussein.gateway.config.AdminBootstrapProperties;
import sn.ussein.gateway.model.Role;
import sn.ussein.gateway.model.User;
import sn.ussein.gateway.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;

/**
 * Cree un compte ADMIN au premier demarrage si aucun n'existe.
 * Identifiants configurables via ADMIN_EMAIL / ADMIN_USERNAME / ADMIN_PASSWORD.
 */
@Component
public class AdminBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapper.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties props;

    public AdminBootstrapper(UserRepository users,
                             PasswordEncoder passwordEncoder,
                             AdminBootstrapProperties props) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean hasAdmin = users.findAll().stream()
            .anyMatch(u -> u.getRoles().contains(Role.ADMIN));
        if (hasAdmin) {
            return;
        }

        if (users.existsByEmail(props.getEmail().toLowerCase())
                || users.existsByUsername(props.getUsername())) {
            log.warn("AdminBootstrapper : un user existe deja avec l'email/username configures " +
                     "mais n'a pas le role ADMIN. Aucune action.");
            return;
        }

        User admin = new User(
            props.getEmail().toLowerCase(),
            props.getUsername(),
            passwordEncoder.encode(props.getPassword())
        );
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        roles.add(Role.ADMIN);
        admin.setRoles(roles);
        users.save(admin);

        log.info("════════════════════════════════════════════");
        log.info("  Compte ADMIN cree : {} / {}", props.getUsername(), props.getEmail());
        log.info("  (mot de passe defini via ADMIN_PASSWORD)");
        log.info("════════════════════════════════════════════");
    }
}
