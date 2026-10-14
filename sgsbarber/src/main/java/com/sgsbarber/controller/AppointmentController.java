package com.sgsbarber.controller;

import com.sgsbarber.model.AppointmentModel;
import com.sgsbarber.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;
import com.sgsbarber.dto.AvailabilityResponse;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService service;
    public AppointmentController(AppointmentService service) { this.service = service; }
    @GetMapping public List<AppointmentModel> list() { return service.listAll(); }
    @GetMapping("/availability")
    public AvailabilityResponse availability(@RequestParam @NonNull Long barberId, @RequestParam @NonNull Long serviceId,
                                             @RequestParam @NonNull LocalDate date) {
        return service.availability(barberId, serviceId, date);
    }
    @GetMapping("/{id}") public AppointmentModel get(@PathVariable @NonNull Long id) { return service.findById(id); }
    @PostMapping public ResponseEntity<AppointmentModel> create(@RequestBody @NonNull AppointmentModel appointment) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(appointment));
    }
    @PutMapping("/{id}") public AppointmentModel update(@PathVariable @NonNull Long id, @RequestBody @NonNull AppointmentModel appointment) {
        return service.update(id, appointment);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @NonNull Long id) { service.delete(id); }
}
