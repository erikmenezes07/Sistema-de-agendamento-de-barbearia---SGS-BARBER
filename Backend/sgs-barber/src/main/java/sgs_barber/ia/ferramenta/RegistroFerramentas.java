package sgs_barber.ia.ferramenta;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sgs_barber.ia.ConversaAgenteException;
import sgs_barber.model.TipoAgente;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registro central das tools. O orquestrador consulta o catálogo para montar o
 * payload de function calling e para executar o que o modelo pediu.
 */
public class RegistroFerramentas {

    private static final Logger log = LoggerFactory.getLogger(RegistroFerramentas.class);

    private final Map<String, Ferramenta> ferramentas = new LinkedHashMap<>();
    private final ObjectMapper objectMapper;

    public RegistroFerramentas(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void add(Ferramenta ferramenta) {
        if (ferramentas.putIfAbsent(ferramenta.nome(), ferramenta) != null) {
            throw new IllegalStateException("Ferramenta duplicada: " + ferramenta.nome());
        }
    }

    public Collection<Ferramenta> todas() {
        return ferramentas.values();
    }

    public List<Ferramenta> disponiveisPara(TipoAgente agente) {
        return ferramentas.values().stream().filter(f -> f.disponivelPara(agente)).toList();
    }

    public Set<String> nomesDisponiveis(TipoAgente agente) {
        Set<String> nomes = new LinkedHashSet<>();
        disponiveisPara(agente).forEach(f -> nomes.add(f.nome()));
        return nomes;
    }

    public List<Map<String, Object>> schemasPara(TipoAgente agente) {
        return disponiveisPara(agente).stream().map(Ferramenta::paraSchema).toList();
    }

    public boolean pertenceAoAgente(String nome, TipoAgente agente) {
        Ferramenta ferramenta = ferramentas.get(nome);
        return ferramenta != null && ferramenta.disponivelPara(agente);
    }

    public boolean existe(String nome) {
        return ferramentas.containsKey(nome);
    }

    /**
     * Executa a tool e devolve JSON. Nunca propaga exceção: um erro de regra de
     * negócio (barbeiro inexistente, horário ocupado) é informação útil para o
     * modelo, que deve explicar o problema ao cliente em vez de travar.
     */
    public String executar(String nome, Map<String, Object> argumentos, TipoAgente agente) {
        Ferramenta ferramenta = ferramentas.get(nome);
        if (ferramenta == null) {
            return json(Map.of("erro", "Ferramenta inexistente: " + nome));
        }
        if (!ferramenta.disponivelPara(agente)) {
            return json(Map.of("erro",
                    "A ferramenta " + nome + " não está disponível para o agente " + agente));
        }

        try {
            Object resultado = ferramenta.executor().executar(argumentos == null ? Map.of() : argumentos);
            return objectMapper.writeValueAsString(resultado);
        } catch (ConversaAgenteException e) {
            // erro esperado de negócio: devolvido ao modelo como feedback
            return json(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            log.error("Falha inesperada na ferramenta {}", nome, e);
            return json(Map.of("erro", "Erro interno ao executar " + nome + ": " + e.getMessage()));
        }
    }

    public String executar(String nome, Map<String, Object> argumentos) {
        return executar(nome, argumentos, null);
    }

    private String json(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (Exception e) {
            return "{\"erro\":\"não foi possível serializar a resposta\"}";
        }
    }
}
