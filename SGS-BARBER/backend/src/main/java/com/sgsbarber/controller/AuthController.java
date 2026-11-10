package com.sgsbarber.controller;

import com.sgsbarber.model.BarberModel;
import com.sgsbarber.model.ClientModel;
import com.sgsbarber.model.UserModel;
import com.sgsbarber.repository.BarberRepository;
import com.sgsbarber.repository.ClientRepository;
import com.sgsbarber.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final ClientRepository clients;
    private final BarberRepository barbers;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UserRepository users, ClientRepository clients, BarberRepository barbers,
                          PasswordEncoder passwordEncoder, SecurityContextRepository securityContextRepository) {
        this.users = users;
        this.clients = clients;
        this.barbers = barbers;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(@NonNull CsrfToken token) {
        return new CsrfResponse(token.getToken());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @NonNull LoginRequest request,
                              HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        String role = normalizeRole(request.role());
        String identifier = normalizeIdentifier(request.identifier());
        UserModel user = users.findByRoleIgnoreCase(role).stream()
                .filter(candidate -> candidate.getActive() == null || candidate.getActive())
                .filter(candidate -> matchesIdentifier(candidate, identifier))
                .findFirst()
                .orElseThrow(this::invalidCredentials);

        if (request.password() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        authenticate(user, servletRequest, servletResponse);
        return response(user);
    }

    @PostMapping("/register")
    @Transactional
    public AuthResponse register(@RequestBody @NonNull RegisterRequest request,
                                HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        String role = normalizeRole(request.role());
        if (!"client".equals(role) && !"barber".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de conta inválido.");
        }
        if (request.name() == null || request.name().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preencha os dados obrigatórios e use uma senha com pelo menos 6 caracteres.");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = request.phone() == null ? "" : request.phone().trim();
        String cpf = digits(request.cpf());
        boolean duplicate = users.findAll().stream().anyMatch(user ->
                email.equalsIgnoreCase(user.getEmail())
                        || (!phone.isBlank() && digits(user.getPhone()).equals(digits(phone)))
                        || (!cpf.isBlank() && digits(user.getCpf()).equals(cpf)));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com esses dados.");
        }

        UserModel user = new UserModel();
        user.setRole(role);
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setCpf(cpf);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);

        if ("client".equals(role)) {
            ClientModel client = new ClientModel();
            client.setName(user.getName());
            client.setPhone(phone);
            client.setEmail(email);
            client.setCpf(cpf);
            client.setBirthDate(request.birthDate());
            user.setClientId(clients.save(client).getId());
        } else {
            BarberModel barber = new BarberModel();
            barber.setName(user.getName());
            barber.setPhone(phone);
            barber.setEmail(email);
            barber.setActive(true);
            user.setBarberId(barbers.save(barber).getId());
        }

        user = users.save(user);
        authenticate(user, servletRequest, servletResponse);
        return response(user);
    }

    @GetMapping("/me")
    public AuthResponse me(Authentication authentication) {
        UserModel user = currentUser(authentication);
        return response(user);
    }

    @PostMapping("/logout")
    public LogoutResponse logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return new LogoutResponse(true);
    }

    private void authenticate(UserModel user, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getId().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase(Locale.ROOT))));
        SecurityContext context = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private UserModel currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada.");
        }
        try {
            return users.findById(Long.valueOf(authentication.getName()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada."));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.", exception);
        }
    }

    private AuthResponse response(UserModel user) {
        return new AuthResponse(true, user.getRole(), new UserResponse(
                user.getId(), user.getRole(), user.getName(), user.getEmail(), user.getPhone(),
                user.getCpf(), user.getClientId(), user.getBarberId()));
    }

    private boolean matchesIdentifier(UserModel user, String identifier) {
        return identifier.equals(normalizeIdentifier(user.getEmail()))
                || identifier.equals(normalizeIdentifier(user.getPhone()))
                || identifier.equals(normalizeIdentifier(user.getCpf()));
    }

    private String normalizeIdentifier(String value) {
        String trimmed = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return trimmed.contains("@") ? trimmed : digits(trimmed);
    }

    private String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        if ("admin".equals(normalized) || "barber".equals(normalized) || "client".equals(normalized)) {
            return normalized;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de conta inválido.");
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Dados de acesso inválidos.");
    }

    public record LoginRequest(String role, String identifier, String password) {}
    public record RegisterRequest(String role, String name, String email, String phone,
                                  String cpf, java.time.LocalDate birthDate, String password) {}
    public record UserResponse(Long id, String role, String name, String email, String phone,
                               String cpf, Long clientId, Long barberId) {}
    public record AuthResponse(boolean ok, String role, UserResponse user) {}
    public record CsrfResponse(String token) {}
    public record LogoutResponse(boolean ok) {}
}
