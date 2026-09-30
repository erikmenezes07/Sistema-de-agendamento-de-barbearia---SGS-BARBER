package sgs_barber.ia;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import sgs_barber.dto.ChatRequestDTO;
import sgs_barber.dto.ChatResponseDTO;
import sgs_barber.ia.agente.Agente;
import sgs_barber.ia.agente.CatalogoAgentes;
import sgs_barber.ia.agente.ContextoSessao;
import sgs_barber.ia.agente.RoteadorAgente;
import sgs_barber.ia.config.IaProperties;
import sgs_barber.ia.ferramenta.FerramentasAgendamento;
import sgs_barber.ia.ferramenta.RegistroFerramentas;
import sgs_barber.model.Conversa;
import sgs_barber.model.PapelMensagem;
import sgs_barber.model.TipoAgente;
import sgs_barber.repository.ClienteRepository;
import sgs_barber.service.ConversaService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestrador da conversa. E o unico lugar que conhece o ciclo completo:
 *
 * <pre>
 *   mensagem -> escolhe o agente -> monta o prompt -> chama o modelo
 *            -> executa as tools pedidas -> devolve o resultado ao modelo
 *            -> repete ate o modelo responder em texto
 * </pre>
 *
 * Decisoes que valem registro:
 * <ul>
 *   <li>A tool {@code encaminhar_para_agente} troca o agente no meio do turno,
 *       o que materializa a arquitetura de dois agentes sem expor isso ao
 *       front-end — o cliente continua falando com um chat so.</li>
 *   <li>O numero de iteracoes e limitado para nao haver custo infinito quando o
 *       modelo entra em laco pedindo ferramenta.</li>
 *   <li>Sem chave de IA, ou com o provedor fora do ar, o turno cai num agente
 *       local de regras: o projeto continua demonstravel sem orcamento de API.</li>
 * </ul>
 */
@Service
public class OrquestradorConversa {

    private static final Logger log = LoggerFactory.getLogger(OrquestradorConversa.class);

    /** Quantas vezes um mesmo turno pode trocar de agente. */
    private static final int MAX_ENCAMINHAMENTOS = 1;

    private static final int TAMANHO_MAXIMO_TRACE = 900;

    private final ObjectProvider<LlmClient> llmClientProvider;
    private final RegistroFerramentas registro;
    private final CatalogoAgentes catalogoAgentes;
    private final RoteadorAgente roteador;
    private final ContextoSessao contextoSessao;
    private final ConversaService conversaService;
    private final ClienteRepository clienteRepository;
    private final IaProperties props;
    private final ObjectMapper objectMapper;

    public OrquestradorConversa(ObjectProvider<LlmClient> llmClientProvider,
                                RegistroFerramentas registro,
                                CatalogoAgentes catalogoAgentes,
                                RoteadorAgente roteador,
                                ContextoSessao contextoSessao,
                                ConversaService conversaService,
                                ClienteRepository clienteRepository,
                                IaProperties props,
                                ObjectMapper objectMapper) {
        this.llmClientProvider = llmClientProvider;
        this.registro = registro;
        this.catalogoAgentes = catalogoAgentes;
        this.roteador = roteador;
        this.contextoSessao = contextoSessao;
        this.conversaService = conversaService;
        this.clienteRepository = clienteRepository;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public ChatResponseDTO responder(ChatRequestDTO request) {
        Conversa conversa = conversaService.obterOuCriar(request.getUsuarioId());
        if (Boolean.TRUE.equals(request.getReiniciar())) {
            conversaService.limpar(conversa);
        }
        conversaService.registrar(conversa, PapelMensagem.USER, request.getMensagem(), null);

        String contexto = contextoSessao.montar(
                nomeDoCliente(request.getClienteId()), request.getClienteId());

        TipoAgente tipoAgente = roteador.resolver(
                request.getMensagem(),
                roteador.doTexto(request.getAgenteDestino()),
                null);

        Trace trace = new Trace();
        String textoFinal = null;
        String aviso = null;
        boolean encaminhou = false;

        for (int passagem = 0; passagem <= MAX_ENCAMINHAMENTOS; passagem++) {
            Agente agente = catalogoAgentes.obter(tipoAgente);

            ResultadoTurno resultado;
            try {
                resultado = executarTurno(agente, contexto, conversa, tipoAgente, trace);
            } catch (LlmException e) {
                log.warn("Provedor de IA indisponivel, usando agente local: {}", e.getMessage());
                AgenteLocalSemChave.RespostaLocal local =
                        AgenteLocalSemChave.responder(request.getMensagem(), registro, tipoAgente);
                resultado = new ResultadoTurno(local.texto(), null);
                // Sem modelo o rastro viria vazio, e o front mostraria um
                // cabecalho "consultou no banco" sem nenhuma tool.
                local.coleta().ferramentas().forEach(trace::ferramentaLocal);
                aviso = "Provedor de IA indisponivel (" + e.getMessage()
                        + "). Resposta montada por regras, sem modelo.";
            }

            if (resultado.encaminharPara() != null && passagem < MAX_ENCAMINHAMENTOS) {
                tipoAgente = resultado.encaminharPara();
                encaminhou = true;
                trace.encaminhamento(agente.tipo(), tipoAgente);
                continue;
            }

            textoFinal = resultado.texto();
            break;
        }

        if (textoFinal == null) {
            textoFinal = "Nao consegui concluir essa conversa agora. Tente novamente em instantes.";
        }

        conversaService.registrar(conversa, PapelMensagem.ASSISTANT, textoFinal, tipoAgente);
        conversaService.trocarAgente(conversa, tipoAgente);

        if (aviso == null && llmClientProvider.getIfAvailable() == null) {
            aviso = "IA_API_KEY nao configurada. A resposta foi montada por regras, sem modelo de linguagem.";
        }

        return new ChatResponseDTO(
                conversa.getId(),
                textoFinal,
                tipoAgente.name(),
                catalogoAgentes.nomeExibicao(tipoAgente),
                trace.ferramentas(),
                encaminhou,
                aviso);
    }

    // ------------------------------------------------------------------

    /**
     * Roda o loop de function calling para um agente ate ele responder em texto.
     */
    private ResultadoTurno executarTurno(Agente agente, String contexto, Conversa conversa,
                                        TipoAgente tipoAgente, Trace trace) {
        LlmClient cliente = llmClientProvider.getIfAvailable();
        // Sem chave nao adianta tentar a chamada: o provedor responderia 401 e
        // o aviso chegaria ao cliente como falha, em vez de "nao configurado".
        if (!props.isHabilitada()) {
            throw new LlmException("Integracao com IA desabilitada (ia.habilitada=false)");
        }
        if (!props.temChaveConfigurada()) {
            throw new LlmException("IA_API_KEY nao configurada");
        }
        if (cliente == null) {
            throw new LlmException("Nenhum cliente de IA configurado");
        }

        List<Map<String, Object>> mensagens = new ArrayList<>();
        mensagens.add(Map.of("role", "system", "content", agente.montarPrompt(contexto)));
        mensagens.addAll(conversaService.historicoParaModelo(
                conversa, props.getMaxMensagensNoContexto(), tipoAgente));

        List<Map<String, Object>> schemas = registro.schemasPara(tipoAgente);
        TipoAgente encaminhamento = null;

        for (int iteracao = 0; iteracao < props.getMaxIteracoes(); iteracao++) {
            LlmClient.RespostaLlm resposta = cliente.completar(mensagens, schemas);
            log.debug("[{}] iteracao {}: {} chamada(s) de ferramenta", tipoAgente, iteracao,
                    resposta.chamadas() == null ? 0 : resposta.chamadas().size());

            if (resposta.somenteTexto()) {
                return new ResultadoTurno(resposta.conteudo(), null);
            }

            List<LlmClient.ChamadaFerramenta> chamadas = resposta.chamadas();
            mensagens.add(mensagemDeChamadas(resposta));

            for (LlmClient.ChamadaFerramenta chamada : chamadas) {
                if (FerramentasAgendamento.ENCAMINHAR.equals(chamada.nome())) {
                    encaminhamento = lerEncaminhamento(chamada.argumentos());
                    trace.encaminhamento(tipoAgente, encaminhamento);
                    continue;
                }

                // Grava no historico antes de executar: se a chamada estourar
                // limite de iteracao, o proximo turno ainda sabe o que foi
                // consultado e o que voltou.
                conversaService.registrarChamada(conversa, tipoAgente, chamada.nome(),
                        escrever(chamada.argumentos()));

                String resultado = registro.executar(chamada.nome(), chamada.argumentos(), tipoAgente);
                trace.ferramenta(chamada, resultado);

                conversaService.registrarResultado(conversa, tipoAgente, chamada.nome(), resultado);
                mensagens.add(mensagemDeResultado(chamada, resultado));
            }

            if (encaminhamento != null) {
                break;
            }
        }

        return new ResultadoTurno(null, encaminhamento);
    }

    private TipoAgente lerEncaminhamento(Map<String, Object> argumentos) {
        Object bruto = argumentos == null ? null : argumentos.get("agente");
        TipoAgente destino = roteador.doTexto(bruto == null ? null : String.valueOf(bruto));
        return destino == null ? TipoAgente.AGENDADOR : destino;
    }

    private Map<String, Object> mensagemDeChamadas(LlmClient.RespostaLlm resposta) {
        List<Map<String, Object>> chamadas = new ArrayList<>();
        for (LlmClient.ChamadaFerramenta c : resposta.chamadas()) {
            Map<String, Object> funcao = new LinkedHashMap<>();
            funcao.put("name", c.nome());
            funcao.put("arguments", escrever(c.argumentos()));
            Map<String, Object> chamada = new LinkedHashMap<>();
            chamada.put("id", c.id() == null ? "call_0" : c.id());
            chamada.put("type", "function");
            chamada.put("function", funcao);
            // O Gemini exige a assinatura de volta nesta mensagem. Sem ela a
            // proxima chamada volta 400 e o agente cai no fallback de regras,
            // parecendo que "a IA nao funciona".
            if (c.assinaturaThought() != null && !c.assinaturaThought().isBlank()) {
                chamada.put("extra_content", Map.of("google", Map.of("thought_signature", c.assinaturaThought())));
            }
            chamadas.add(chamada);
        }

        Map<String, Object> mensagem = new LinkedHashMap<>();
        mensagem.put("role", "assistant");
        mensagem.put("content", resposta.conteudo() == null ? "" : resposta.conteudo());
        mensagem.put("tool_calls", chamadas);
        return mensagem;
    }

    private Map<String, Object> mensagemDeResultado(LlmClient.ChamadaFerramenta chamada, String resultado) {
        Map<String, Object> mensagem = new LinkedHashMap<>();
        mensagem.put("role", "tool");
        mensagem.put("tool_call_id", chamada.id() == null ? "call_0" : chamada.id());
        mensagem.put("name", chamada.nome());
        mensagem.put("content", resultado);
        return mensagem;
    }

    private String nomeDoCliente(Long clienteId) {
        if (clienteId == null) {
            return null;
        }
        return clienteRepository.findById(clienteId).map(c -> c.getNome()).orElse(null);
    }

    private String escrever(Map<String, Object> argumentos) {
        try {
            return objectMapper.writeValueAsString(argumentos == null ? Map.of() : argumentos);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Resultado de um turno: ou o texto final, ou o pedido de passagem. */
    private record ResultadoTurno(String texto, TipoAgente encaminharPara) {
    }

    /**
     * Tudo que o agente consultou no banco durante a conversa. E o que permite
     * ao front-end mostrar a origem das informacoes, em vez de apenas repassar
     * texto gerado.
     */
    public static final class Trace {

        private final List<Map<String, Object>> ferramentas = new ArrayList<>();

        void ferramenta(LlmClient.ChamadaFerramenta chamada, String resultado) {
            ferramentas.add(Map.of(
                    "nome", chamada.nome(),
                    "argumentos", chamada.argumentos() == null ? Map.of() : chamada.argumentos(),
                    "resultado", truncar(resultado)));
        }

        void encaminhamento(TipoAgente de, TipoAgente para) {
            ferramentas.add(Map.of(
                    "nome", FerramentasAgendamento.ENCAMINHAR,
                    "resultado", "passagem de " + de + " para " + para));
        }

        /** Tool chamada pelo agente local, sem passar pelo modelo. */
        void ferramentaLocal(Map<String, Object> item) {
            ferramentas.add(item);
        }

        List<Map<String, Object>> ferramentas() {
            return ferramentas;
        }

        private static String truncar(String json) {
            if (json == null) {
                return "";
            }
            return json.length() <= TAMANHO_MAXIMO_TRACE ? json : json.substring(0, TAMANHO_MAXIMO_TRACE) + "... (truncado)";
        }
    }
}
