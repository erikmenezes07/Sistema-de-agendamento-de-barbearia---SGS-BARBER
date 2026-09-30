package sgs_barber.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sgs_barber.dto.HorarioBarbeiroDTO;
import sgs_barber.service.HorarioBarbeiroService;

import java.util.List;

@RestController
@RequestMapping("/api/horarios-barbeiro")
public class HorarioBarbeiroController {

    private final HorarioBarbeiroService service;

    public HorarioBarbeiroController(HorarioBarbeiroService service) {
        this.service = service;
    }

    @GetMapping("/barbeiro/{barbeiroId}")
    public ResponseEntity<List<HorarioBarbeiroDTO>> listarPorBarbeiro(@PathVariable Long barbeiroId) {
        return ResponseEntity.ok(service.listarPorBarbeiro(barbeiroId));
    }

    @PostMapping
    public ResponseEntity<HorarioBarbeiroDTO> criar(@Valid @RequestBody HorarioBarbeiroDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
