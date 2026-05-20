package sn.ussein.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.ussein.gateway.dto.AdminJobView;
import sn.ussein.gateway.dto.AdminStats;
import sn.ussein.gateway.dto.AdminUserUpdateRequest;
import sn.ussein.gateway.dto.AdminUserView;
import sn.ussein.gateway.security.Identity;
import sn.ussein.gateway.security.IdentityResolver;
import sn.ussein.gateway.service.AdminService;

import java.util.Map;

/**
 * Endpoints reserves au role ADMIN (protege par SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;
    private final IdentityResolver identity;

    public AdminController(AdminService admin, IdentityResolver identity) {
        this.admin = admin;
        this.identity = identity;
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return admin.stats();
    }

    @GetMapping("/users")
    public Map<String, Object> listUsers(@RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Page<AdminUserView> p = admin.listUsers(q, page, size);
        return Map.of(
            "content", p.getContent(),
            "page", p.getNumber(),
            "size", p.getSize(),
            "totalElements", p.getTotalElements(),
            "totalPages", p.getTotalPages()
        );
    }

    @PatchMapping("/users/{id}")
    public AdminUserView updateUser(@PathVariable String id,
                                    @RequestBody AdminUserUpdateRequest req,
                                    HttpServletRequest http) {
        Identity me = identity.current(http);
        return admin.updateUser(id, req, me);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id, HttpServletRequest http) {
        Identity me = identity.current(http);
        admin.deleteUser(id, me);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/jobs")
    public Map<String, Object> listJobs(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        Page<AdminJobView> p = admin.listJobs(page, size);
        return Map.of(
            "content", p.getContent(),
            "page", p.getNumber(),
            "size", p.getSize(),
            "totalElements", p.getTotalElements(),
            "totalPages", p.getTotalPages()
        );
    }
}
