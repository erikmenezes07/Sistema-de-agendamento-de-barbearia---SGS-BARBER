package com.sgsbarber.controller;

import com.sgsbarber.model.BarberModel;
import com.sgsbarber.service.BarberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import java.util.List;

@RestController
@RequestMapping("/api/barbers")
public class BarberController {

    private final BarberService barberService;

    public BarberController(BarberService barberService) {
        this.barberService = barberService;
    }

    @GetMapping
    public ResponseEntity<List<BarberModel>> listBarbers() {
        return ResponseEntity.ok(barberService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarberModel> getBarber(@PathVariable @NonNull Long id) {
        return ResponseEntity.ok(barberService.findById(id));
    }

    @PostMapping
    public ResponseEntity<BarberModel> createBarber(@RequestBody @NonNull BarberModel barber) {
        BarberModel saved = barberService.create(barber);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BarberModel> updateBarber(@PathVariable @NonNull Long id, @RequestBody @NonNull BarberModel barber) {
        return ResponseEntity.ok(barberService.update(id, barber));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBarber(@PathVariable @NonNull Long id) { barberService.delete(id); }
}
