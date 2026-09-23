package sgs_barber.service;

import sgs_barber.dto.ServicoDTO;
import sgs_barber.model.Servico;
import sgs_barber.repository.ServicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServicoService {

    private final ServicoRepository repository;

    public ServicoService(ServicoRepository repository) {
        this.repository = repository;
    }

    public List<ServicoDTO> listarTodos() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ServicoDTO buscarPorId(Long id) {
        Servico servico = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado com ID: " + id));
        return toDTO(servico);
    }

    public ServicoDTO salvar(ServicoDTO dto) {
        Servico servico = toEntity(dto);
        return toDTO(repository.save(servico));
    }

    public ServicoDTO atualizar(Long id, ServicoDTO dto) {
        Servico servico = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado com ID: " + id));
        servico.setNome(dto.getNome());
        servico.setPreco(dto.getPreco());
        servico.setDuracaoMinutos(dto.getDuracaoMinutos());
        return toDTO(repository.save(servico));
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }

    private ServicoDTO toDTO(Servico servico) {
        ServicoDTO dto = new ServicoDTO();
        dto.setId(servico.getId());
        dto.setNome(servico.getNome());
        dto.setPreco(servico.getPreco());
        dto.setDuracaoMinutos(servico.getDuracaoMinutos());
        return dto;
    }

    private Servico toEntity(ServicoDTO dto) {
        Servico servico = new Servico();
        servico.setId(dto.getId());
        servico.setNome(dto.getNome());
        servico.setPreco(dto.getPreco());
        servico.setDuracaoMinutos(dto.getDuracaoMinutos());
        return servico;
    }
}