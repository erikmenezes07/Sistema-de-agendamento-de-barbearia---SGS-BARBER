package sgs_barber.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sgs_barber.model.Conversa;
import sgs_barber.model.MensagemConversa;
import sgs_barber.model.PapelMensagem;
import sgs_barber.model.TipoAgente;
import sgs_barber.repository.ConversaRepository;
import sgs_barber.repository.MensagemConversaRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Memoria de curto prazo dos agentes.
 *
 * Guarda o historico no Postgres e o reconstroi no formato de chat da API de
 * function calling, incluindo as chamadas de ferramenta. Sem essa reconstrução
 * o modelo "esquece" o que acabou de consultar e volta ao mesmo horario com
 * nomes inventados.
 */
@Service
public class ConversaService {

    private final ConversaRepository conversaRepository;
    private final MensagemConversaRepository mensagemRepository;

    public ConversaService(ConversaRepository conversaRepository,
                           MensagemConversaRepository mensagemRepository) {
        this.conversaRepository = conversaRepository;
        this.mensagemRepository = mensagemRepository;
    }

    @Transactional
    public Conversa obterOuCriar(String usuarioId) {
        return conversaRepository.findFirstByUsuarioIdOrderByAtualizadaEmDesc(usuarioId)
                .orElseGet(() -> {
                    Conversa nova = new Conversa();
                    nova.setUsuarioId(usuarioId);
                    nova.setAgenteAtual(TipoAgente.AGENDADOR);
                    return conversaRepository.save(nova);
                });
    }

    @Transactional
    public MensagemConversa registrar(Conversa conversa, PapelMensagem papel, String conteudo,
                                      TipoAgente agente) {
        return mensagemRepository.save(montar(conversa, papel, conteudo, agente, null, null));
    }

    /**
     * Registra a intenção do modelo de chamar uma ferramenta. O texto
     * argumentos vai em JSON, como a API espera.
     */
    @Transactional
    public MensagemConversa registrarChamada(Conversa conversa, TipoAgente agente,
                                              String nomeFerramenta, String argumentos) {
        return mensagemRepository.save(
                montar(conversa, PapelMensagem.ASSISTANT, null, agente, nomeFerramenta, argumentos));
    }

    @Transactional
    public MensagemConversa registrarResultado(Conversa conversa, TipoAgente agente,
                                                String nomeFerramenta, String resultado) {
        return mensagemRepository.save(
                montar(conversa, PapelMensagem.TOOL, resultado, agente, nomeFerramenta, null));
    }

    @Transactional
    public void trocarAgente(Conversa conversa, TipoAgente agente) {
        if (agente != null && conversa.getAgenteAtual() != agente) {
            conversa.setAgenteAtual(agente);
            conversaRepository.save(conversa);
        }
    }

    /**
     * Historico no formato de mensagens para o modelo.
     *
     * As mensagens de ferramenta so entram quando pertencem ao agente que esta
     * respondendo. Isso importa: um tool_call de {@code criar_agendamento} na
     * conversa seria rejeitado pela API se fosse reenviado para o Recomendador,
     * que nao tem essa ferramenta.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> historicoParaModelo(Conversa conversa, int limite, TipoAgente agenteAtual) {
        List<MensagemConversa> todas = mensagemRepository.findByConversaIdOrderByOrdemAsc(conversa.getId());
        List<MensagemConversa> janela = ultimasMensagens(todas, limite, agenteAtual);

        List<Map<String, Object>> saida = new ArrayList<>(janela.size());
        for (MensagemConversa m : janela) {
            saida.add(paraModelo(m));
        }
        return saida;
    }

    /** Historico legivel, para o front-end recarregar a conversa. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> historicoParaFront(Conversa conversa) {
        List<MensagemConversa> mensagens = mensagemRepository.findByConversaIdOrderByOrdemAsc(conversa.getId());
        List<Map<String, Object>> saida = new ArrayList<>();

        for (MensagemConversa m : mensagens) {
            boolean ehRespostaDoModelo = m.getPapel() == PapelMensagem.ASSISTANT
                    && m.getFerramentaNome() == null;
            if (m.getPapel() != PapelMensagem.USER && !ehRespostaDoModelo) {
                continue;
            }
            if (m.getConteudo() == null || m.getConteudo().isBlank()) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ordem", m.getOrdem());
            item.put("papel", m.getPapel().name().toLowerCase());
            item.put("conteudo", m.getConteudo());
            item.put("agente", m.getAgente() == null ? null : m.getAgente().name());
            saida.add(item);
        }
        return saida;
    }

    @Transactional
    public void limpar(Conversa conversa) {
        mensagemRepository.deleteByConversaId(conversa.getId());
    }

    /**
     * Recorta a janela mais recente sem quebrar pares tool_call/tool, e sem
     * deixar uma mensagem de ferramenta sem a chamada que a originou.
     */
    private List<MensagemConversa> ultimasMensagens(List<MensagemConversa> mensagens, int limite,
                                                    TipoAgente agenteAtual) {
        List<MensagemConversa> filtradas = new ArrayList<>();
        for (MensagemConversa m : mensagens) {
            if (m.getPapel() == PapelMensagem.TOOL || ehChamadaDeFerramenta(m)) {
                if (m.getAgente() == agenteAtual) {
                    filtradas.add(m);
                }
            } else {
                filtradas.add(m);
            }
        }

        if (filtradas.size() <= limite) {
            return filtradas;
        }

        int inicio = filtradas.size() - limite;
        while (inicio < filtradas.size() && ehChamadaDeFerramenta(filtradas.get(inicio))) {
            inicio++;
        }
        return new ArrayList<>(filtradas.subList(inicio, filtradas.size()));
    }

    private boolean ehChamadaDeFerramenta(MensagemConversa m) {
        return m.getPapel() == PapelMensagem.ASSISTANT && m.getFerramentaNome() != null;
    }

    /**
     * O id do tool_call e derivado do campo `ordem`, que ja e sequencial e
     * unico. O resultado da tool recebe ordem+1, e por isso o id volta uma
     * posicao — é assim que o par se reencontra.
     */
    private Map<String, Object> paraModelo(MensagemConversa m) {
        Map<String, Object> mensagem = new LinkedHashMap<>();
        mensagem.put("role", m.getPapel().name().toLowerCase());

        if (ehChamadaDeFerramenta(m)) {
            Map<String, Object> funcao = new LinkedHashMap<>();
            funcao.put("name", m.getFerramentaNome());
            funcao.put("arguments", m.getFerramentaArgumentos() == null ? "{}" : m.getFerramentaArgumentos());

            mensagem.put("content", m.getConteudo() == null ? "" : m.getConteudo());
            mensagem.put("tool_calls", List.of(Map.of(
                    "id", idDeChamada(m.getOrdem()),
                    "type", "function",
                    "function", funcao)));
            return mensagem;
        }

        if (m.getPapel() == PapelMensagem.TOOL) {
            mensagem.put("tool_call_id", idDeChamada(m.getOrdem() - 1));
            mensagem.put("name", m.getFerramentaNome());
            mensagem.put("content", m.getConteudo());
            return mensagem;
        }

        mensagem.put("content", m.getConteudo());
        return mensagem;
    }

    private String idDeChamada(Integer ordem) {
        return "call_" + (ordem == null ? 0 : ordem);
    }

    private MensagemConversa montar(Conversa conversa, PapelMensagem papel, String conteudo,
                                    TipoAgente agente, String ferramentaNome, String argumentos) {
        MensagemConversa m = new MensagemConversa();
        m.setConversa(conversa);
        m.setOrdem(proximaOrdem(conversa));
        m.setPapel(papel);
        m.setConteudo(conteudo);
        m.setAgente(agente);
        m.setFerramentaNome(ferramentaNome);
        m.setFerramentaArgumentos(argumentos);
        return m;
    }

    private int proximaOrdem(Conversa conversa) {
        return mensagemRepository.findByConversaIdOrderByOrdemAsc(conversa.getId()).stream()
                .mapToInt(m -> m.getOrdem() == null ? 0 : m.getOrdem())
                .max()
                .orElse(0) + 1;
    }
}
