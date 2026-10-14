package com.sgsbarber.controller;

import com.sgsbarber.model.ClientModel;
import com.sgsbarber.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
    private final ClientService clientService;

    public ClientController(ClientService clientService) { this.clientService = clientService; }

    @GetMapping public List<ClientModel> list() { return clientService.listAll(); }
    @GetMapping("/{id}") public ClientModel get(@PathVariable @NonNull Long id) { return clientService.findById(id); }
    @PostMapping public ResponseEntity<ClientModel> create(@RequestBody @NonNull ClientModel client) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(client));
    }
    @PutMapping("/{id}") public ClientModel update(@PathVariable @NonNull Long id, @RequestBody @NonNull ClientModel client) {
        return clientService.update(id, client);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @NonNull Long id) { clientService.delete(id); }
}
