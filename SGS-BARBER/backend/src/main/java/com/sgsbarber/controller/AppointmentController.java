package com.sgsbarber.controller;

import com.sgsbarber.model.AppointmentModel;
import com.sgsbarber.model.UserModel;
import com.sgsbarber.repository.UserRepository;
import com.sgsbarber.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDate;
import com.sgsbarber.dto.AvailabilityResponse;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService service;
    private final UserRepository users;
    public AppointmentController(AppointmentService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }
    @GetMapping
    public List<AppointmentModel> list(Authentication authentication) {
        UserModel user = currentUser(authentication);
        if ("admin".equals(user.getRole())) return service.listAll();
        if ("barber".equals(user.getRole()) && user.getBarberId() != null) return service.listByBarberId(user.getBarberId());
        if ("client".equals(user.getRole()) && user.getClientId() != null) return service.listByClientId(user.getClientId());
        return List.of();
    }
    @GetMapping("/availability")
    public AvailabilityResponse availability(@RequestParam @NonNull Long barberId, @RequestParam @NonNull Long serviceId,
                                             @RequestParam @NonNull LocalDate date) {
        return service.availability(barberId, serviceId, date);
    }
    @GetMapping("/{id}")
    public AppointmentModel get(@PathVariable @NonNull Long id, Authentication authentication) {
        UserModel user = currentUser(authentication);
        AppointmentModel appointment = service.findById(id);
        requireAccess(user, appointment);
        return appointment;
    }
    @PostMapping
    public ResponseEntity<AppointmentModel> create(@RequestBody @NonNull AppointmentModel appointment,
                                                   Authentication authentication) {
        UserModel user = currentUser(authentication);
        if ("client".equals(user.getRole())) {
            appointment.setClientId(user.getClientId());
        } else if ("barber".equals(user.getRole())) {
            appointment.setBarberId(user.getBarberId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(appointment));
    }
    @PutMapping("/{id}")
    public AppointmentModel update(@PathVariable @NonNull Long id, @RequestBody @NonNull AppointmentModel appointment,
                                   Authentication authentication) {
        UserModel user = currentUser(authentication);
        requireAccess(user, service.findById(id));
        if ("barber".equals(user.getRole())) appointment.setBarberId(user.getBarberId());
        return service.update(id, appointment);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @NonNull Long id, Authentication authentication) {
        requireAccess(currentUser(authentication), service.findById(id));
        service.delete(id);
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

    private void requireAccess(UserModel user, AppointmentModel appointment) {
        boolean canAccess = "admin".equals(user.getRole())
                || ("barber".equals(user.getRole()) && user.getBarberId() != null
                && user.getBarberId().equals(appointment.getBarberId()))
                || ("client".equals(user.getRole()) && user.getClientId() != null
                && user.getClientId().equals(appointment.getClientId()));
        if (!canAccess) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado.");
    }
}
