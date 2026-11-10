package com.sgsbarber.controller;

import com.sgsbarber.model.AppointmentModel;
import com.sgsbarber.model.BarberModel;
import com.sgsbarber.model.UserModel;
import com.sgsbarber.repository.AppointmentRepository;
import com.sgsbarber.repository.BarberRepository;
import com.sgsbarber.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final AppointmentRepository appointments;
    private final UserRepository users;
    private final BarberRepository barbers;

    public DashboardController(AppointmentRepository appointments, UserRepository users, BarberRepository barbers) {
        this.appointments = appointments;
        this.users = users;
        this.barbers = barbers;
    }

    @GetMapping("/summary")
    public DashboardSummary summary(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada.");
        }
        UserModel user;
        try {
            user = users.findById(Long.valueOf(authentication.getName()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada."));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.", exception);
        }
        List<AppointmentModel> rows;
        BigDecimal commissionPercent = new BigDecimal("50");
        if ("barber".equals(user.getRole()) && user.getBarberId() != null) {
            rows = appointments.findByBarberId(user.getBarberId());
            BarberModel barber = barbers.findById(user.getBarberId()).orElse(null);
            if (barber != null && barber.getCommission() != null) commissionPercent = barber.getCommission();
        } else {
            rows = appointments.findAll();
        }
        final BigDecimal commissionRate = commissionPercent;
        List<AppointmentModel> completed = rows.stream()
                .filter(appointment -> isCompleted(appointment.getStatus()))
                .toList();
        BigDecimal gross = completed.stream()
                .map(appointment -> zero(appointment.getPrice()).add(zero(appointment.getExtrasTotal())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commission = completed.stream()
                .map(appointment -> zero(appointment.getPrice()).multiply(commissionRate)
                        .divide(new BigDecimal("100"))
                        .add(zero(appointment.getExtrasTotal()).multiply(new BigDecimal("0.2"))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardSummary(gross, commission, completed.size());
    }

    private boolean isCompleted(String status) {
        return status != null && ("concluído".equalsIgnoreCase(status)
                || "concluido".equalsIgnoreCase(status)
                || "completed".equalsIgnoreCase(status));
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record DashboardSummary(BigDecimal gross, BigDecimal commission, int completed) {}
}
