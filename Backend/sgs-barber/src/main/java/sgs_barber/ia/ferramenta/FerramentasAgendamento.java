package sgs_barber.ia.ferramenta;

import org.springframework.stereotype.Component;
import sgs_barber.dto.AgendamentoDTO;
import sgs_barber.ia.ConversaAgenteException;
import sgs_barber.ia.util.Argumentos;
import sgs_barber.ia.util.Datas;
import sgs_barber.model.Barbeiro;
import sgs_barber.model.Cliente;
import sgs_barber.model.Servico;
import sgs_barber.model.TipoAgente;
import sgs_barber.repository.BarbeiroRepository;
import sgs_barber.repository.ClienteRepository;
import sgs_barber.repository.ServicoRepository;
import sgs_barber.service.AgendamentoService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static sgs_barber.ia.ferramenta.Esquema.booleano;
import static sgs_barber.ia.ferramenta.Esquema.inteiro;
import static sgs_barber.ia.ferramenta.Esquema.listaDeInteiros;
import static sgs_barber.ia.ferramenta.Esquema.objeto;
import static sgs_barber.ia.ferramenta.Esquema.opcao;
import static sgs_barber.ia.ferramenta.Esquema.string;

/**
 * Tools de escrita. Pertencem exclusivamente ao Agente Agendador: e ele quem
 * altera a agenda. O Agente Recomendador recebe apenas leitura, o que impede
 * que ele crie reservas sem o cliente pedir.
 */
@Component
public class FerramentasAgendamento implements ConjuntoDeFerramentas {

    public static final String VERIFICAR_DISPONIBILIDADE = "verificar_disponibilidade";
    public static final String CRIAR_AGENDAMENTO = "criar_agendamento";
    public static final String LISTAR_AGENDAMENTOS = "listar_agendamentos";
    public static final String CANCELAR_AGENDAMENTO = "cancelar_agendamento";

    /** Nome reservado ao roteamento entre agentes; nao consulta o banco. */
    public static final String ENCAMINHAR = "encaminhar_para_agente";

    private final AgendamentoService agendamentoService;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;

    public FerramentasAgendamento(AgendamentoService agendamentoService,
                                  BarbeiroRepository barbeiroRepository,
                                  ClienteRepository clienteRepository,
                                  ServicoRepository servicoRepository) {
        this.agendamentoService = agendamentoService;
        this.barbeiroRepository = barbeiroRepository;
        this.clienteRepository = clienteRepository;
        this.servicoRepository = servicoRepository;
    }

    @Override
    public void registrar(RegistroFerramentas registro) {
        registro.add(new Ferramenta(
                VERIFICAR_DISPONIBILIDADE,
                "Cruza a agenda do barbeiro com a duracao desejada e devolve os horarios livres. "
                        + "Aceita expressoes naturais de data ('sexta', 'amanha') e devolve o dia da semana, "
                        + "o expediente, o intervalo de almoco e a lista de slots. "
                        + "Consulte sempre antes de criar um agendamento.",
                objeto(
                        string("barbeiro_id", "Id do barbeiro.", true),
                        string("data", "Data desejada: AAAA-MM-DD, 'hoje', 'amanha' ou dia da semana.", true),
                        inteiro("duracao_total_minutos", "Soma das duracoes dos servicos.", true),
                        string("hora_inicio", "Hora desejada (ex: '14:00' ou '14h'). Opcional.", false),
                        string("periodo", "Filtro por periodo: 'manha', 'tarde' ou 'noite'. Opcional.", false),
                        inteiro("dias_a_consultar", "Quantos dias consecutivos consultar a partir de 'data'. Padrao 1.", false)),
                Set.of(TipoAgente.AGENDADOR),
                this::verificarDisponibilidade));

        registro.add(new Ferramenta(
                CRIAR_AGENDAMENTO,
                "Grava a reserva e bloqueia o horario na agenda. So chame depois de verificar_disponibilidade "
                        + "e de o cliente ter confirmado. Aceita varios servico_ids para combos "
                        + "(a agenda e reservada uma vez pelo tempo total).",
                objeto(
                        string("cliente_id", "Id do cliente. Se ausente, use cliente_nome ou cliente_telefone.", false),
                        string("cliente_nome", "Nome do cliente, se nao houver id.", false),
                        string("cliente_telefone", "Telefone do cliente, se nao houver id.", false),
                        string("barbeiro_id", "Id do barbeiro.", true),
                        listaDeInteiros("servico_ids", "Ids dos servicos. Mais de um forma um combo.", false),
                        string("servico_id", "Id de um unico servico. Alternativa a servico_ids.", false),
                        string("data", "Data desejada: AAAA-MM-DD, 'amanha' ou dia da semana.", true),
                        string("hora_inicio", "Hora desejada: '14:00' ou '14h'.", true),
                        string("observacao", "Observacao livre para a agenda.", false)),
                Set.of(TipoAgente.AGENDADOR),
                this::criarAgendamento));

        registro.add(new Ferramenta(
                LISTAR_AGENDAMENTOS,
                "Lista os agendamentos de um cliente ou de um barbeiro, para o cliente perguntar "
                        + "'quando eu tenho marcado?' ou o barbeiro conferir a agenda do dia.",
                objeto(
                        string("cliente_id", "Id do cliente. Tem prioridade sobre barbeiro_id.", false),
                        string("barbeiro_id", "Id do barbeiro.", false),
                        string("data", "Filtra por uma data especifica. Opcional.", false),
                        booleano("incluir_cancelados", "Se true, inclui cancelados. Padrao false.", false)),
                Set.of(TipoAgente.AGENDADOR),
                this::listarAgendamentos));

        registro.add(new Ferramenta(
                CANCELAR_AGENDAMENTO,
                "Cancela um agendamento existente e libera o horario. "
                        + "Confirme com o cliente antes de chamar.",
                objeto(string("agendamento_id", "Id do agendamento a cancelar.", true)),
                Set.of(TipoAgente.AGENDADOR),
                this::cancelarAgendamento));

        registro.add(new Ferramenta(
                ENCAMINHAR,
                "Repassa a conversa para o outro agente quando o pedido nao e da sua competencia. "
                        + "Use 'recomendador' para estilo, servico ou escolha de barbeiro por especialidade; "
                        + "use 'agendador' para horarios e reservas.",
                objeto(
                        opcao("agente", "Agente que deve continuar a conversa.", true, "agendador", "recomendador"),
                        string("motivo", "Resumo do que o cliente pediu.", true)),
                Set.of(TipoAgente.AGENDADOR, TipoAgente.RECOMENDADOR),
                args -> {
                    throw new ConversaAgenteException("Encaminhamento e tratado pelo orquestrador, nao pela tool.");
                }));
    }

    // ------------------------------------------------------------------

    private Object verificarDisponibilidade(Map<String, Object> args) {
        Long barbeiroId = Argumentos.idObrigatorio(args, "barbeiro_id");
        Barbeiro barbeiro = barbeiroRepository.findById(barbeiroId)
                .orElseThrow(() -> new ConversaAgenteException("Barbeiro com id " + barbeiroId + " nao existe."));

        LocalDate dataInicial = Datas.exigirData(Argumentos.texto(args, "data"), "data");
        int duracao = Argumentos.inteiro(args, "duracao_total_minutos", 0);
        if (duracao <= 0) {
            throw new ConversaAgenteException(
                    "Informe 'duracao_total_minutos' maior que zero. Use obter_tempo_servico para descobrir.");
        }

        LocalTime horaInicio = Argumentos.hora(args, "hora_inicio");
        String periodo = Argumentos.texto(args, "periodo");
        LocalTime[] janela = Datas.janelaDoDia(periodo);
        if (horaInicio == null && janela != null) {
            horaInicio = janela[0];
        }

        int dias = Argumentos.inteiro(args, "dias_a_consultar", 1);
        dias = Math.max(1, Math.min(dias, 14));

        List<Map<String, Object>> dias_ = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            LocalDate data = dataInicial.plusDays(i);
            var disponibilidade = agendamentoService.consultarDisponibilidade(barbeiroId, data, null, duracao);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("data", disponibilidade.getData());
            item.put("dia_semana", disponibilidade.getDiaSemana());
            item.put("atende_neste_dia", disponibilidade.isAtendeNesteDia());
            item.put("expediente", disponibilidade.getTurnos());
            item.put("horario_almoco", disponibilidade.getHorarioAlmoco());
            item.put("total_slots_livres", disponibilidade.getTotalSlotsLivres());

            // Filtra pela janela pedida ("a tarde") sem perder a contagem real.
            List<String> slots = disponibilidade.getSlotsLivres().stream()
                    .filter(s -> janela == null || Datas.dentroDaJanela(s, janela[0], janela[1]))
                    .map(s -> s.toString().substring(0, 5))
                    .toList();
            item.put("horarios_livres", slots);
            item.put("total_na_janela_pedida", slots.size());

            if (horaInicio != null) {
                item.put("hora_pedida", horaInicio.toString().substring(0, 5));
                item.put("hora_pedida_disponivel",
                        slots.contains(horaInicio.toString().substring(0, 5)));
            }

            dias_.add(item);
        }

        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("barbeiro_id", barbeiroId);
        saida.put("barbeiro_nome", barbeiro.getNome());
        saida.put("duracao_total_minutos", duracao);
        saida.put("dias", dias_);
        return saida;
    }

    private Object criarAgendamento(Map<String, Object> args) {
        Long barbeiroId = Argumentos.idObrigatorio(args, "barbeiro_id");
        Cliente cliente = resolverCliente(args);

        List<Long> servicoIds = new ArrayList<>(Argumentos.listaDeIds(args, "servico_ids"));
        Long servicoUnico = Argumentos.id(args, "servico_id");
        if (servicoUnico != null && !servicoIds.contains(servicoUnico)) {
            servicoIds.add(0, servicoUnico);
        }
        if (servicoIds.isEmpty()) {
            throw new ConversaAgenteException("Informe 'servico_id' ou 'servico_ids' com ao menos um servico.");
        }

        List<Servico> servicos = new ArrayList<>();
        for (Long id : servicoIds) {
            servicos.add(servicoRepository.findById(id)
                    .orElseThrow(() -> new ConversaAgenteException("Servico com id " + id + " nao existe.")));
        }

        LocalDate data = Datas.exigirData(Argumentos.texto(args, "data"), "data");
        LocalTime hora = Argumentos.hora(args, "hora_inicio");
        if (hora == null) {
            throw new ConversaAgenteException("Informe 'hora_inicio' no formato HH:mm ou HHh (ex: '14:00' ou '14h').");
        }
        if (data.isBefore(LocalDate.now())) {
            throw new ConversaAgenteException(
                    "A data " + data + " ja passou. Ofereca as proximas datas disponiveis.");
        }

        AgendamentoDTO dto = new AgendamentoDTO();
        dto.setClienteId(cliente.getId());
        dto.setBarbeiroId(barbeiroId);
        dto.setServicoId(servicos.get(0).getId());
        dto.setDataAgendamento(data);
        dto.setHoraInicio(hora);
        dto.setObservacao(Argumentos.texto(args, "observacao"));

        AgendamentoDTO criado = agendamentoService.criarAgendamento(dto, servicos);

        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("agendamento_id", criado.getId());
        saida.put("cliente", criado.getClienteNome());
        saida.put("barbeiro", criado.getBarbeiroNome());
        saida.put("servicos", servicos.stream().map(Servico::getNome).toList());
        saida.put("data", criado.getDataAgendamento().toString());
        saida.put("dia_semana", Datas.nomeDoDia(criado.getDataAgendamento()));
        saida.put("hora_inicio", criado.getHoraInicio().toString().substring(0, 5));
        saida.put("hora_fim", criado.getHoraFim().toString().substring(0, 5));
        saida.put("duracao_minutos", criado.getDuracaoMinutos());
        saida.put("status", criado.getStatus().name());
        saida.put("confirmacao", "Agendamento confirmado. Confirme ao cliente com data, hora e barbeiro.");
        return saida;
    }

    private Object listarAgendamentos(Map<String, Object> args) {
        boolean incluirCancelados = Boolean.TRUE.equals(Argumentos.booleano(args, "incluir_cancelados", false));
        Long clienteId = Argumentos.id(args, "cliente_id");
        Long barbeiroId = Argumentos.id(args, "barbeiro_id");
        LocalDate data = Argumentos.data(args, "data");

        List<AgendamentoDTO> agendamentos;
        if (clienteId != null) {
            agendamentos = agendamentoService.listarPorCliente(clienteId);
        } else if (barbeiroId != null && data != null) {
            agendamentos = agendamentoService.listarPorBarbeiroEData(barbeiroId, data);
        } else if (barbeiroId != null) {
            agendamentos = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate d = (data != null ? data : LocalDate.now()).plusDays(i);
                agendamentos.addAll(agendamentoService.listarPorBarbeiroEData(barbeiroId, d));
            }
        } else {
            throw new ConversaAgenteException("Informe 'cliente_id' ou 'barbeiro_id' para listar agendamentos.");
        }

        if (data != null) {
            LocalDate filtro = data;
            agendamentos = agendamentos.stream().filter(a -> a.getDataAgendamento().equals(filtro)).toList();
        }
        if (!incluirCancelados) {
            List<AgendamentoDTO> filtrados = new ArrayList<>();
            for (AgendamentoDTO a : agendamentos) {
                if (a.getStatus() != sgs_barber.model.StatusAgendamento.CANCELADO) {
                    filtrados.add(a);
                }
            }
            agendamentos = filtrados;
        }

        List<Map<String, Object>> resposta = new ArrayList<>();
        for (AgendamentoDTO a : agendamentos) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("cliente", a.getClienteNome());
            item.put("barbeiro", a.getBarbeiroNome());
            item.put("servico", a.getServicoNome());
            item.put("data", a.getDataAgendamento().toString());
            item.put("dia_semana", Datas.nomeDoDia(a.getDataAgendamento()));
            item.put("hora_inicio", a.getHoraInicio().toString().substring(0, 5));
            item.put("hora_fim", a.getHoraFim().toString().substring(0, 5));
            item.put("status", a.getStatus().name());
            resposta.add(item);
        }

        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("quantidade", resposta.size());
        saida.put("agendamentos", resposta);
        return saida;
    }

    private Object cancelarAgendamento(Map<String, Object> args) {
        Long id = Argumentos.idObrigatorio(args, "agendamento_id");
        AgendamentoDTO cancelado = agendamentoService.cancelar(id);

        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("agendamento_id", cancelado.getId());
        saida.put("status", cancelado.getStatus().name());
        saida.put("barbeiro", cancelado.getBarbeiroNome());
        saida.put("data", cancelado.getDataAgendamento().toString());
        saida.put("hora_inicio", cancelado.getHoraInicio().toString().substring(0, 5));
        saida.put("confirmacao", "Agendamento cancelado e horario liberado.");
        return saida;
    }

    private Cliente resolverCliente(Map<String, Object> args) {
        Long id = Argumentos.id(args, "cliente_id");
        if (id != null) {
            return clienteRepository.findById(id)
                    .orElseThrow(() -> new ConversaAgenteException("Cliente com id " + id + " nao existe."));
        }

        String telefone = Argumentos.texto(args, "cliente_telefone");
        if (!telefone.isBlank()) {
            String digitos = telefone.replaceAll("\\D+", "");
            Optional<Cliente> porTelefone = clienteRepository.findAll().stream()
                    .filter(c -> c.getTelefone().replaceAll("\\D+", "").equals(digitos))
                    .findFirst();
            if (porTelefone.isPresent()) {
                return porTelefone.get();
            }
            throw new ConversaAgenteException("Nenhum cliente cadastrado com o telefone " + telefone + ".");
        }

        String nome = Argumentos.texto(args, "cliente_nome");
        if (!nome.isBlank()) {
            List<Cliente> encontrados = clienteRepository.findAll().stream()
                    .filter(c -> c.getNome().toLowerCase().contains(nome.toLowerCase()))
                    .toList();
            if (encontrados.size() == 1) {
                return encontrados.get(0);
            }
            if (encontrados.size() > 1) {
                throw new ConversaAgenteException("Mais de um cliente com o nome '" + nome
                        + "'. Pergunte o telefone para diferenciar.");
            }
            throw new ConversaAgenteException("Nenhum cliente encontrado para '" + nome
                    + "'. Pergunte nome, telefone e email para cadastrar.");
        }

        throw new ConversaAgenteException(
                "Nao foi possivel identificar o cliente. Informe 'cliente_id', ou 'cliente_telefone', "
                        + "ou 'cliente_nome'.");
    }
}
