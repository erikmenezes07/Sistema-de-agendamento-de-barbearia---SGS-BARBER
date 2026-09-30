package sgs_barber.service;

import org.springframework.stereotype.Service;
import sgs_barber.dto.HorarioBarbeiroDTO;
import sgs_barber.model.Barbeiro;
import sgs_barber.model.HorarioBarbeiro;
import sgs_barber.repository.BarbeiroRepository;
import sgs_barber.repository.HorarioBarbeiroRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HorarioBarbeiroService {

    private final HorarioBarbeiroRepository repository;
    private final BarbeiroRepository barbeiroRepository;

    public HorarioBarbeiroService(HorarioBarbeiroRepository repository, BarbeiroRepository barbeiroRepository) {
        this.repository = repository;
        this.barbeiroRepository = barbeiroRepository;
    }

    public List<HorarioBarbeiroDTO> listarPorBarbeiro(Long barbeiroId) {
        return repository.findByBarbeiroId(barbeiroId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public HorarioBarbeiroDTO salvar(HorarioBarbeiroDTO dto) {
        Barbeiro barbeiro = barbeiroRepository.findById(dto.getBarbeiroId())
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado com ID: " + dto.getBarbeiroId()));

        HorarioBarbeiro horario = new HorarioBarbeiro();
        horario.setBarbeiro(barbeiro);
        horario.setDiaSemana(dto.getDiaSemana());
        horario.setHoraInicio(dto.getHoraInicio());
        horario.setHoraFim(dto.getHoraFim());
        horario.setAlmocoInicio(dto.getAlmocoInicio());
        horario.setAlmocoFim(dto.getAlmocoFim());

        return toDTO(repository.save(horario));
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }

    private HorarioBarbeiroDTO toDTO(HorarioBarbeiro horario) {
        HorarioBarbeiroDTO dto = new HorarioBarbeiroDTO();
        dto.setId(horario.getId());
        dto.setBarbeiroId(horario.getBarbeiro().getId());
        dto.setDiaSemana(horario.getDiaSemana());
        dto.setHoraInicio(horario.getHoraInicio());
        dto.setHoraFim(horario.getHoraFim());
        dto.setAlmocoInicio(horario.getAlmocoInicio());
        dto.setAlmocoFim(horario.getAlmocoFim());
        return dto;
    }
}
