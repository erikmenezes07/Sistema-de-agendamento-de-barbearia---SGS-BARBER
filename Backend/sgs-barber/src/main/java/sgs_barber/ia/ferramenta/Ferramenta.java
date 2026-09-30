package sgs_barber.ia.ferramenta;

import sgs_barber.model.TipoAgente;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Definição de uma ferramenta (function) exposta ao modelo.
 *
 * A propriedade {@code agentes} é o que materializa a separação de
 * responsabilidades: cada tool declara quem pode usá-la, e o orquestrador só
 * envia ao modelo as tools do agente que está respondendo naquele turno.
 */
public record Ferramenta(
        String nome,
        String descricao,
        Map<String, Object> parametros,
        java.util.Set<TipoAgente> agentes,
        Executor executor
) {

    /** Implementação da tool. Recebe os argumentos já desserializados. */
    @FunctionalInterface
    public interface Executor {
        Object executar(Map<String, Object> argumentos);
    }

    /** Monta a tool no formato esperado pela API de function calling. */
    public Map<String, Object> paraSchema() {
        Map<String, Object> funcao = new LinkedHashMap<>();
        funcao.put("name", nome);
        funcao.put("description", descricao);
        funcao.put("parameters", parametros == null || parametros.isEmpty() ? schemaVazio() : parametros);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", funcao);
        return tool;
    }

    public boolean disponivelPara(TipoAgente agente) {
        // Set.of(...) nao aceita null e lanca NullPointerException em contains(null).
        return agente != null && agentes.contains(agente);
    }

    private static Map<String, Object> schemaVazio() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of());
        return schema;
    }
}
