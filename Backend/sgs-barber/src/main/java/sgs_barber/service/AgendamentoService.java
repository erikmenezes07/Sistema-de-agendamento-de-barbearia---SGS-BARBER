package sgs_barber.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sgs_barber.dto.AgendamentoDTO;
import sgs_barber.dto.DisponibilidadeDetalhadaDTO;
import sgs_barber.dto.DisponibilidadeResponseDTO;
import sgs_barber.model.*;
import sgs_barber.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgendamentoService {

    private final AgendamentoRepository repository;
    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ServicoRepository servicoRepository;
    private final HorarioBarbeiroRepository horarioBarbeiroRepository;

    // Granularidade usada para sugerir horários alternativos (em minutos)
    private static final int INTERVALO_SUGESTAO_MINUTOS = 15;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] NOMES_DIAS = {
            "", "segunda-feira", "terça-feira", "quarta-feira",
            "quinta-feira", "sexta-feira", "sábado", "domingo"
    };

    public AgendamentoService(AgendamentoRepository repository,
                              ClienteRepository clienteRepository,
                              BarbeiroRepository barbeiroRepository,
                              ServicoRepository servicoRepository,
                              HorarioBarbeiroRepository horarioBarbeiroRepository) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.barbeiroRepository = barbeiroRepository;
        this.servicoRepository = servicoRepository;
        this.horarioBarbeiroRepository = horarioBarbeiroRepository;
    }

    public List<AgendamentoDTO> listarPorCliente(Long clienteId) {
        return repository.findByClienteId(clienteId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<AgendamentoDTO> listarPorBarbeiroEData(Long barbeiroId, LocalDate data) {
        return repository.findByBarbeiroIdAndDataAgendamento(barbeiroId, data)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Endpoint que a IA (ou qualquer front) chama antes de marcar: pergunta
     * "esse barbeiro está livre nesse dia/hora para esse serviço?"
     */
    public DisponibilidadeResponseDTO verificarDisponibilidade(Long barbeiroId, LocalDate data,
                                                                LocalTime horaInicio, Integer duracaoMinutos) {
        LocalTime horaFim = horaInicio.plusMinutes(duracaoMinutos);
        int diaSemana = data.getDayOfWeek().getValue(); // 1 = segunda ... 7 = domingo

        List<HorarioBarbeiro> turnos = horarioBarbeiroRepository.findByBarbeiroIdAndDiaSemana(barbeiroId, diaSemana);
        if (turnos.isEmpty()) {
            return new DisponibilidadeResponseDTO(false, "O barbeiro não atende neste dia da semana", List.of());
        }

        boolean dentroDoExpediente = turnos.stream()
                .anyMatch(t -> cabeNoTurno(t, horaInicio, horaFim));
        if (!dentroDoExpediente) {
            return new DisponibilidadeResponseDTO(false, "Fora do horário de expediente do barbeiro",
                    listarHorariosLivres(barbeiroId, data, duracaoMinutos));
        }

        boolean conflito = haConflito(barbeiroId, data, horaInicio, horaFim, null);
        if (conflito) {
            return new DisponibilidadeResponseDTO(false, "Já existe um agendamento nesse horário",
                    listarHorariosLivres(barbeiroId, data, duracaoMinutos));
        }

        return new DisponibilidadeResponseDTO(true, null, List.of());
    }

    /**
     * Versão completa usada pelo Agente 1: devolve contexto do dia (turnos,
     * almoço) e a lista de todos os slots livres, já filtrada.
     *
     * @param horaInicio se null, apenas lista os slots livres do dia inteiro
     */
    public DisponibilidadeDetalhadaDTO consultarDisponibilidade(Long barbeiroId, LocalDate data,
                                                                 LocalTime horaInicio, Integer duracaoMinutos) {
        Barbeiro barbeiro = barbeiroRepository.findById(barbeiroId)
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado com ID: " + barbeiroId));

        int diaSemana = data.getDayOfWeek().getValue();
        List<HorarioBarbeiro> turnos = horarioBarbeiroRepository.findByBarbeiroIdAndDiaSemana(barbeiroId, diaSemana);
        List<LocalTime> livres = listarHorariosLivres(barbeiroId, data, duracaoMinutos);

        DisponibilidadeDetalhadaDTO dto = new DisponibilidadeDetalhadaDTO();
        dto.setBarbeiroId(barbeiroId);
        dto.setBarbeiroNome(barbeiro.getNome());
        dto.setData(data.toString());
        dto.setDiaSemana(NOMES_DIAS[diaSemana]);
        dto.setDuracaoMinutos(duracaoMinutos);
        dto.setAtendeNesteDia(!turnos.isEmpty());
        dto.setTurnos(turnos.stream()
                .map(t -> t.getHoraInicio().format(FORMATO_HORA) + " às " + t.getHoraFim().format(FORMATO_HORA))
                .collect(Collectors.toList()));
        dto.setHorarioAlmoco(turnos.stream()
                .filter(t -> !t.isSemAlmoco())
                .map(t -> t.getAlmocoInicio().format(FORMATO_HORA) + " às " + t.getAlmocoFim().format(FORMATO_HORA))
                .findFirst()
                .orElse(null));
        dto.setSlotsLivres(livres);
        dto.setTotalSlotsLivres(livres.size());

        if (turnos.isEmpty()) {
            dto.setDisponivel(false);
            dto.setMotivo("O barbeiro não atende neste dia da semana");
            return dto;
        }

        if (horaInicio == null) {
            dto.setDisponivel(!livres.isEmpty());
            dto.setMotivo(livres.isEmpty() ? "Agenda cheia neste dia" : null);
            return dto;
        }

        LocalTime horaFim = horaInicio.plusMinutes(duracaoMinutos);
        boolean cabe = turnos.stream().anyMatch(t -> cabeNoTurno(t, horaInicio, horaFim));
        if (!cabe) {
            dto.setDisponivel(false);
            boolean invadeAlmoco = turnos.stream().anyMatch(t -> invadePausa(t, horaInicio, horaFim));
            dto.setMotivo(invadeAlmoco
                    ? "O barbeiro está em pausa de almoço nesse horário"
                    : "Fora do horário de expediente do barbeiro");
            return dto;
        }
        if (haConflito(barbeiroId, data, horaInicio, horaFim, null)) {
            dto.setDisponivel(false);
            dto.setMotivo("Já existe um agendamento nesse horário");
            return dto;
        }

        dto.setDisponivel(true);
        return dto;
    }

    /**
     * Consulta vários dias de uma vez. É o que permite ao agente responder
     * "tem horário sexta ou sábado?" sem disparar uma tool por dia.
     */
    public List<DisponibilidadeDetalhadaDTO> consultarDisponibilidadePorIntervalo(
            Long barbeiroId, LocalDate dataInicial, int quantidadeDias, Integer duracaoMinutos) {
        List<DisponibilidadeDetalhadaDTO> resultado = new ArrayList<>();
        for (int i = 0; i < quantidadeDias; i++) {
            resultado.add(consultarDisponibilidade(barbeiroId, dataInicial.plusDays(i), null, duracaoMinutos));
        }
        return resultado;
    }

    /**
     * Entrada usada pelo controller. Precisa de {@code @Transactional} aqui
     * porque a chamada a {@link #criarAgendamento} é auto-invocação: o proxy
     * do Spring não é acionado e o {@code @Transactional} dela não valeria.
     * Sem transação o lock pessimista do barbeiro falha com
     * "No active transaction" e a agenda fica sem proteção contra corrida.
     */
    @Transactional
    public AgendamentoDTO salvar(AgendamentoDTO dto) {
        Servico servico = servicoRepository.findById(dto.getServicoId())
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado com ID: " + dto.getServicoId()));
        AgendamentoDTO combo = new AgendamentoDTO();
        combo.setClienteId(dto.getClienteId());
        combo.setBarbeiroId(dto.getBarbeiroId());
        combo.setServicoId(dto.getServicoId());
        combo.setDataAgendamento(dto.getDataAgendamento());
        combo.setHoraInicio(dto.getHoraInicio());
        combo.setObservacao(dto.getObservacao());
        return criarAgendamento(combo, List.of(servico));
    }

    /**
     * Cria a reserva. Aceita mais de um serviço para os combos ("corte + barba"):
     * a agenda do barbeiro é reservada uma única vez, pelo tempo total, e a
     * lista de serviços fica registrada em `observacao`.
     */
    @Transactional
    public AgendamentoDTO criarAgendamento(AgendamentoDTO dto, List<Servico> servicos) {
        if (servicos == null || servicos.isEmpty()) {
            throw new RuntimeException("Informe ao menos um serviço para o agendamento");
        }

        // Trava a agenda do barbeiro para impedir dois agendamentos simultâneos
        // no mesmo horário (barbeiro é a unidade de contenção da concorrência).
        Barbeiro barbeiro = barbeiroRepository.findByIdComLock(dto.getBarbeiroId())
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado com ID: " + dto.getBarbeiroId()));
        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado com ID: " + dto.getClienteId()));

        int duracaoTotal = servicos.stream().mapToInt(Servico::getDuracaoMinutos).sum();
        LocalTime horaFim = dto.getHoraInicio().plusMinutes(duracaoTotal);

        DisponibilidadeDetalhadaDTO disponibilidade = consultarDisponibilidade(
                dto.getBarbeiroId(), dto.getDataAgendamento(), dto.getHoraInicio(), duracaoTotal);
        if (!disponibilidade.isDisponivel()) {
            throw new RuntimeException("Horário indisponível: " + disponibilidade.getMotivo());
        }

        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setBarbeiro(barbeiro);
        // Serviço principal do combo: o primeiro da lista. Os demais ficam na observação.
        agendamento.setServico(servicos.get(0));
        agendamento.setDataAgendamento(dto.getDataAgendamento());
        agendamento.setHoraInicio(dto.getHoraInicio());
        agendamento.setHoraFim(horaFim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setObservacao(montarObservacao(dto.getObservacao(), servicos));

        return toDTO(repository.save(agendamento));
    }

    @Transactional
    public AgendamentoDTO cancelar(Long id) {
        Agendamento agendamento = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado com ID: " + id));
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        return toDTO(repository.save(agendamento));
    }

    private String montarObservacao(String observacao, List<Servico> servicos) {
        String listaServicos = servicos.stream()
                .map(s -> s.getNome() + " (" + s.getDuracaoMinutos() + "min)")
                .collect(Collectors.joining(" + "));
        if (servicos.size() <= 1) {
            return observacao;
        }
        String prefixo = "Serviços: " + listaServicos;
        return (observacao == null || observacao.isBlank()) ? prefixo : prefixo + " | " + observacao;
    }

    private boolean haConflito(Long barbeiroId, LocalDate data, LocalTime inicio, LocalTime fim, Long ignorarId) {
        List<Agendamento> doDia = repository.findByBarbeiroIdAndDataAgendamentoAndStatusNot(
                barbeiroId, data, StatusAgendamento.CANCELADO);

        return doDia.stream()
                .filter(a -> ignorarId == null || !a.getId().equals(ignorarId))
                // dois intervalos se sobrepõem quando um começa antes do outro terminar, nos dois sentidos
                .anyMatch(a -> inicio.isBefore(a.getHoraFim()) && a.getHoraInicio().isBefore(fim));
    }

    private List<LocalTime> listarHorariosLivres(Long barbeiroId, LocalDate data, Integer duracaoMinutos) {
        int diaSemana = data.getDayOfWeek().getValue();
        List<HorarioBarbeiro> turnos = horarioBarbeiroRepository.findByBarbeiroIdAndDiaSemana(barbeiroId, diaSemana);

        boolean hoje = data.equals(LocalDate.now());

        List<LocalTime> livres = new ArrayList<>();
        for (HorarioBarbeiro turno : turnos) {
            LocalTime cursor = turno.getHoraInicio();
            while (!cursor.plusMinutes(duracaoMinutos).isAfter(turno.getHoraFim())) {
                LocalTime fimSlot = cursor.plusMinutes(duracaoMinutos);
                if (cabeNoTurno(turno, cursor, fimSlot)
                        && !haConflito(barbeiroId, data, cursor, fimSlot, null)
                        // não oferece horário que já passou
                        && !(hoje && !cursor.isAfter(LocalTime.now()))) {
                    livres.add(cursor);
                }
                cursor = cursor.plusMinutes(INTERVALO_SUGESTAO_MINUTOS);
            }
        }
        return livres;
    }

    /** O serviço cabe no turno sem invadir a pausa de almoço? */
    private boolean cabeNoTurno(HorarioBarbeiro turno, LocalTime inicio, LocalTime fim) {
        if (inicio.isBefore(turno.getHoraInicio()) || fim.isAfter(turno.getHoraFim())) {
            return false;
        }
        if (turno.isSemAlmoco()) {
            return true;
        }
        // Sobrepõe o almoço? (intervalos semiabertos [inicio, fim))
        return !iniciaAntes(inicio, turno.getAlmocoFim()) || !terminaDepois(fim, turno.getAlmocoInicio());
    }

    /** O intervalo cai dentro da pausa, mas respeita o expediente? */
    private boolean invadePausa(HorarioBarbeiro turno, LocalTime inicio, LocalTime fim) {
        if (turno.isSemAlmoco()) {
            return false;
        }
        if (inicio.isBefore(turno.getHoraInicio()) || fim.isAfter(turno.getHoraFim())) {
            return false;
        }
        return !iniciaAntes(inicio, turno.getAlmocoFim()) && !terminaDepois(fim, turno.getAlmocoInicio());
    }

    private boolean iniciaAntes(LocalTime inicio, LocalTime limite) {
        return inicio.isBefore(limite);
    }

    private boolean terminaDepois(LocalTime fim, LocalTime limite) {
        return fim.isAfter(limite);
    }

    public static String nomeDoDia(LocalDate data) {
        return NOMES_DIAS[data.getDayOfWeek().getValue()];
    }

    private AgendamentoDTO toDTO(Agendamento a) {
        AgendamentoDTO dto = new AgendamentoDTO();
        dto.setId(a.getId());
        dto.setClienteId(a.getCliente().getId());
        dto.setClienteNome(a.getCliente().getNome());
        dto.setBarbeiroId(a.getBarbeiro().getId());
        dto.setBarbeiroNome(a.getBarbeiro().getNome());
        dto.setServicoId(a.getServico().getId());
        dto.setServicoNome(a.getServico().getNome());
        dto.setDataAgendamento(a.getDataAgendamento());
        dto.setHoraInicio(a.getHoraInicio());
        dto.setHoraFim(a.getHoraFim());
        dto.setStatus(a.getStatus());
        dto.setObservacao(a.getObservacao());
        dto.setDataCriacao(a.getDataCriacao());
        return dto;
    }
}
