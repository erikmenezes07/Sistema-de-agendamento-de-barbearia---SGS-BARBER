package sgs_barber.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sgs_barber.dto.AgendamentoDTO;
import sgs_barber.dto.DisponibilidadeResponseDTO;
import sgs_barber.service.AgendamentoService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoService service;

    public AgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    /**
     * Endpoint pensado pro agente de IA: recebe barbeiro + data + hora + duração
     * do serviço e diz se está livre (e, se não estiver, sugere horários livres).
     *
     * Exemplo: GET /api/agendamentos/disponibilidade?barbeiroId=1&data=2026-10-05&horaInicio=14:00&duracaoMinutos=30
     */
    @GetMapping("/disponibilidade")
    public ResponseEntity<DisponibilidadeResponseDTO> verificarDisponibilidade(
            @RequestParam Long barbeiroId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaInicio,
            @RequestParam Integer duracaoMinutos) {
        return ResponseEntity.ok(service.verificarDisponibilidade(barbeiroId, data, horaInicio, duracaoMinutos));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<AgendamentoDTO>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.listarPorCliente(clienteId));
    }

    @GetMapping("/barbeiro/{barbeiroId}")
    public ResponseEntity<List<AgendamentoDTO>> listarPorBarbeiroEData(
            @PathVariable Long barbeiroId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(service.listarPorBarbeiroEData(barbeiroId, data));
    }

    @PostMapping
    public ResponseEntity<AgendamentoDTO> criar(@Valid @RequestBody AgendamentoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(dto));
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelar(id));
    }
}
