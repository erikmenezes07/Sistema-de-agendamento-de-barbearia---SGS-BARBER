package sgs_barber.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sgs_barber.dto.ChatRequestDTO;
import sgs_barber.dto.ChatResponseDTO;
import sgs_barber.ia.OrquestradorConversa;
import sgs_barber.ia.agente.CatalogoAgentes;
import sgs_barber.ia.config.IaProperties;
import sgs_barber.ia.ferramenta.RegistroFerramentas;
import sgs_barber.model.Conversa;
import sgs_barber.model.TipoAgente;
import sgs_barber.service.ConversaService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Porta de entrada do modulo de IA.
 *
 * O front-end conversa apenas com este controller. A chave da IA, o prompt e o
 * catalogo de ferramentas nunca saem do back-end — se o navegador chamasse a
 * API do modelo diretamente, qualquer visitante poderia ler a chave e criar
 * agendamentos na base.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final OrquestradorConversa orquestrador;
    private final ConversaService conversaService;
    private final RegistroFerramentas registro;
    private final CatalogoAgentes catalogoAgentes;
    private final IaProperties props;

    public ChatController(OrquestradorConversa orquestrador,
                          ConversaService conversaService,
                          RegistroFerramentas registro,
                          CatalogoAgentes catalogoAgentes,
                          IaProperties props) {
        this.orquestrador = orquestrador;
        this.conversaService = conversaService;
        this.registro = registro;
        this.catalogoAgentes = catalogoAgentes;
        this.props = props;
    }

    /**
     * Envia uma mensagem e devolve a resposta do agente, ja em linguagem
     * natural. O campo {@code ferramentas_usadas} mostra quais consultas foram
     * realmente feitas ao banco.
     *
     * Quando {@code ia.agendador-ativo=false} o endpoint responde 503 em vez de
     * cair no agente local: quem desligou a IA no servidor nao espera receber
     * resposta montada por regras em lugar da do modelo.
     */
    @PostMapping
    public ResponseEntity<ChatResponseDTO> enviar(@Valid @RequestBody ChatRequestDTO request) {
        if (!props.isAgendadorAtivo()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O assistente de IA esta desativado neste servidor (ia.agendador-ativo=false).");
        }
        return ResponseEntity.ok(orquestrador.responder(request));
    }

    /** Historico legivel da conversa do usuario. */
    @GetMapping("/historico/{usuarioId}")
    public ResponseEntity<List<Map<String, Object>>> historico(@PathVariable String usuarioId) {
        Conversa conversa = conversationOf(usuarioId);
        return ResponseEntity.ok(conversaService.historicoParaFront(conversa));
    }

    @PostMapping("/reiniciar/{usuarioId}")
    public ResponseEntity<Map<String, Object>> reiniciar(@PathVariable String usuarioId) {
        Conversa conversa = conversaService.obterOuCriar(usuarioId);
        conversaService.limpar(conversa);
        return ResponseEntity.ok(Map.of("mensagem", "Conversa reiniciada", "conversa_id", conversa.getId()));
    }

    /**
     * Diagnostico da integracao: mostra as tools de cada agente e diz se a IA
     * esta de fato ativa. Ajuda a provar, na apresentacao do projeto, que a
     * separacao de responsabilidades existe no codigo.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> configuracao() {
        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("agendador_ativo", props.isAgendadorAtivo());
        saida.put("ia_habilitada", props.isHabilitada());
        saida.put("modelo", props.getModelo());
        saida.put("provedor", props.getBaseUrl());
        saida.put("chave_configurada", props.temChaveConfigurada());
        saida.put("fallback_sem_chave", props.isPermitirFallbackSemChave());
        saida.put("max_iteracoes", props.getMaxIteracoes());

        Map<String, Object> agentes = new LinkedHashMap<>();
        for (TipoAgente tipo : TipoAgente.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nome", catalogoAgentes.nomeExibicao(tipo));
            item.put("ferramentas", registro.nomesDisponiveis(tipo));
            agentes.put(tipo.name(), item);
        }
        saida.put("agentes", agentes);
        return ResponseEntity.ok(saida);
    }

    private Conversa conversationOf(String usuarioId) {
        return conversaService.obterOuCriar(usuarioId);
    }
}
