package com.sgsbarber.controller;

import com.sgsbarber.model.BarberModel;
import com.sgsbarber.model.ClientModel;
import com.sgsbarber.model.UserModel;
import com.sgsbarber.repository.BarberRepository;
import com.sgsbarber.repository.ClientRepository;
import com.sgsbarber.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final ClientRepository clients;
    private final BarberRepository barbers;

    public AuthController(UserRepository users, ClientRepository clients, BarberRepository barbers) {
        this.users = users;
        this.clients = clients;
        this.barbers = barbers;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        String role = text(body.get("role")).toLowerCase();
        String identifier = text(body.get("identifier"));
        String password = text(body.get("password"));

        if (!List.of("client", "barber", "admin").contains(role)) {
            return error(HttpStatus.BAD_REQUEST, "Tipo de usuário inválido.");
        }

        UserModel user = users.findAll().stream()
                .filter(item -> role.equalsIgnoreCase(item.getRole()))
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .filter(item -> matchesIdentifier(item, identifier))
                .findFirst()
                .orElse(null);

        if (user == null || !passwordMatches(password, user.getPasswordHash())) {
            return error(HttpStatus.UNAUTHORIZED, "Dados de acesso inválidos.");
        }

        return ResponseEntity.ok(Map.of("ok", true, "role", user.getRole(), "user", publicUser(user)));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        String role = text(body.get("role")).toLowerCase();
        if (!List.of("client", "barber").contains(role)) {
            return error(HttpStatus.BAD_REQUEST, "Tipo de cadastro inválido.");
        }

        String name = text(body.get("name"));
        String email = text(body.get("email")).toLowerCase();
        String phone = text(body.get("phone"));
        String cpf = digits(text(body.get("cpf")));
        String password = text(body.get("password"));
        if (name.isBlank() || email.isBlank() || password.length() < 6) {
            return error(HttpStatus.BAD_REQUEST, "Preencha nome, e-mail e uma senha com pelo menos 6 caracteres.");
        }

        boolean duplicate = users.findAll().stream().anyMatch(item ->
                email.equalsIgnoreCase(text(item.getEmail()))
                        || (!phone.isBlank() && phone.equals(text(item.getPhone())))
                        || (!cpf.isBlank() && cpf.equals(digits(item.getCpf()))));
        if (duplicate) return error(HttpStatus.CONFLICT, "Já existe uma conta com esses dados.");

        UserModel user = new UserModel();
        user.setRole(role);
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setCpf(cpf);
        user.setPasswordHash(hash(password));
        user.setActive(true);

        if ("client".equals(role)) {
            ClientModel client = new ClientModel();
            client.setName(name);
            client.setEmail(email);
            client.setPhone(phone);
            client.setCpf(cpf);
            String birthDate = text(body.get("birthDate"));
            if (!birthDate.isBlank()) client.setBirthDate(LocalDate.parse(birthDate));
            user.setClientId(clients.save(client).getId());
        } else {
            BarberModel barber = new BarberModel(name, email, phone, true);
            barber.setCommission(java.math.BigDecimal.valueOf(50));
            user.setBarberId(barbers.save(barber).getId());
        }

        UserModel saved = users.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("ok", true, "role", saved.getRole(), "user", publicUser(saved)));
    }

    private boolean matchesIdentifier(UserModel user, String identifier) {
        String value = text(identifier).toLowerCase();
        String numeric = digits(value);
        return value.equals(text(user.getEmail()).toLowerCase())
                || value.equals(text(user.getPhone()).toLowerCase())
                || value.equals(text(user.getCpf()).toLowerCase())
                || (!numeric.isBlank() && numeric.equals(digits(user.getPhone())))
                || (!numeric.isBlank() && numeric.equals(digits(user.getCpf())));
    }

    private Map<String, Object> publicUser(UserModel user) {
        return Map.of("id", user.getId(), "role", user.getRole(), "name", user.getName(),
                "email", user.getEmail(), "phone", text(user.getPhone()), "cpf", text(user.getCpf()),
                "clientId", user.getClientId() == null ? 0 : user.getClientId(),
                "barberId", user.getBarberId() == null ? 0 : user.getBarberId());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("ok", false, "message", message));
    }

    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private static String digits(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }

    private static boolean passwordMatches(String password, String stored) {
        return stored != null && MessageDigest.isEqual(hash(password).getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8));
    }
}
