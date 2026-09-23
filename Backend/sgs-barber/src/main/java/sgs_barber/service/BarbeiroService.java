package sgs_barber.service;

import sgs_barber.dto.BarbeiroDTO;
import sgs_barber.model.Barbeiro;
import sgs_barber.repository.BarbeiroRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BarbeiroService {

    private final BarbeiroRepository repository;

    public BarbeiroService(BarbeiroRepository repository) {
        this.repository = repository;
    }

    public List<BarbeiroDTO> listarTodos() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public BarbeiroDTO buscarPorId(Long id) {
        Barbeiro barbeiro = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado com ID: " + id));
        return toDTO(barbeiro);
    }

    public BarbeiroDTO salvar(BarbeiroDTO dto) {
        Barbeiro barbeiro = toEntity(dto);
        return toDTO(repository.save(barbeiro));
    }

    public BarbeiroDTO atualizar(Long id, BarbeiroDTO dto) {
        Barbeiro barbeiro = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado com ID: " + id));
        barbeiro.setNome(dto.getNome());
        barbeiro.setTelefone(dto.getTelefone());
        barbeiro.setAtivo(dto.getAtivo());
        return toDTO(repository.save(barbeiro));
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }

    private BarbeiroDTO toDTO(Barbeiro barbeiro) {
        BarbeiroDTO dto = new BarbeiroDTO();
        dto.setId(barbeiro.getId());
        dto.setNome(barbeiro.getNome());
        dto.setTelefone(barbeiro.getTelefone());
        dto.setAtivo(barbeiro.getAtivo());
        return dto;
    }

    private Barbeiro toEntity(BarbeiroDTO dto) {
        Barbeiro barbeiro = new Barbeiro();
        barbeiro.setId(dto.getId());
        barbeiro.setNome(dto.getNome());
        barbeiro.setTelefone(dto.getTelefone());
        barbeiro.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        return barbeiro;
    }
}