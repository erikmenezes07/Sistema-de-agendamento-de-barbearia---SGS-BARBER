package com.sgsbarber.controller;

import com.sgsbarber.model.ServiceModel;
import com.sgsbarber.service.ServiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ServiceController {

    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceModel>> listServices() {
        return ResponseEntity.ok(serviceService.listAll());
    }

    @GetMapping("/services/{id}")
    public ResponseEntity<ServiceModel> getService(@PathVariable @NonNull Long id) {
        return ResponseEntity.ok(serviceService.findById(id));
    }

    @PostMapping("/services")
    public ResponseEntity<ServiceModel> createService(@RequestBody @NonNull ServiceModel service) {
        ServiceModel saved = serviceService.create(service);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<ServiceModel> updateService(@PathVariable @NonNull Long id, @RequestBody @NonNull ServiceModel service) {
        return ResponseEntity.ok(serviceService.update(id, service));
    }

    @DeleteMapping("/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable @NonNull Long id) { serviceService.delete(id); }
}
