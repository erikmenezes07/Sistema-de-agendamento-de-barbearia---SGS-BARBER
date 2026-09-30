package sgs_barber.ia.ferramenta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Atalho para escrever os contratos JSON Schema das tools sem ruído.
 *
 * Ex.:
 * <pre>
 * Esquema.objeto(
 *     Esquema.string("barbeiro_id", "id do barbeiro", true),
 *     Esquema.inteiro("duracao_total_minutos", "minutos", true))
 * </pre>
 */
public final class Esquema {

    private Esquema() {
    }

    public static Map<String, Object> objeto(Map.Entry<String, Object>... campos) {
        Map<String, Object> propriedades = new LinkedHashMap<>();
        List<String> obrigatorias = new java.util.ArrayList<>();
        for (Map.Entry<String, Object> campo : campos) {
            if (campo == null) {
                continue;
            }
            propriedades.put(campo.getKey(), campo.getValue());
            if (Boolean.TRUE.equals(((Map<?, ?>) campo.getValue()).get("obrigatorio"))) {
                obrigatorias.add(campo.getKey());
            }
        }

        Map<String, Object> semChaveObrigatoria = new LinkedHashMap<>();
        propriedades.forEach((chave, valor) -> {
            Map<String, Object> copia = new LinkedHashMap<>((Map<String, Object>) valor);
            copia.remove("obrigatorio");
            semChaveObrigatoria.put(chave, copia);
        });

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", semChaveObrigatoria);
        if (!obrigatorias.isEmpty()) {
            schema.put("required", obrigatorias);
        }
        return schema;
    }

    public static Map<String, Object> semArgumentos() {
        return objeto();
    }

    public static Map.Entry<String, Object> string(String nome, String descricao, boolean obrigatorio) {
        return Map.entry(nome, base("string", descricao, obrigatorio));
    }

    public static Map.Entry<String, Object> string(String nome, String descricao) {
        return string(nome, descricao, false);
    }

    public static Map.Entry<String, Object> inteiro(String nome, String descricao, boolean obrigatorio) {
        return Map.entry(nome, base("integer", descricao, obrigatorio));
    }

    public static Map.Entry<String, Object> inteiro(String nome, String descricao) {
        return inteiro(nome, descricao, false);
    }

    public static Map.Entry<String, Object> numero(String nome, String descricao, boolean obrigatorio) {
        return Map.entry(nome, base("number", descricao, obrigatorio));
    }

    public static Map.Entry<String, Object> numero(String nome, String descricao) {
        return numero(nome, descricao, false);
    }

    public static Map.Entry<String, Object> booleano(String nome, String descricao, boolean obrigatorio) {
        return Map.entry(nome, base("boolean", descricao, obrigatorio));
    }

    public static Map.Entry<String, Object> booleano(String nome, String descricao) {
        return booleano(nome, descricao, false);
    }

    public static Map.Entry<String, Object> listaDeInteiros(String nome, String descricao, boolean obrigatorio) {
        Map<String, Object> corpo = new LinkedHashMap<>(base("array", descricao, obrigatorio));
        corpo.put("items", Map.of("type", "integer"));
        return Map.entry(nome, corpo);
    }

    public static Map.Entry<String, Object> listaDeTexto(String nome, String descricao, boolean obrigatorio) {
        Map<String, Object> corpo = new LinkedHashMap<>(base("array", descricao, obrigatorio));
        corpo.put("items", Map.of("type", "string"));
        return Map.entry(nome, corpo);
    }

    /** Enum: o modelo fica restrito aos valores permitidos. */
    public static Map.Entry<String, Object> opcao(String nome, String descricao, boolean obrigatorio,
                                                 String... valores) {
        Map<String, Object> corpo = new LinkedHashMap<>(base("string", descricao, obrigatorio));
        corpo.put("enum", List.of(valores));
        return Map.entry(nome, corpo);
    }

    private static Map<String, Object> base(String tipo, String descricao, boolean obrigatorio) {
        Map<String, Object> campo = new LinkedHashMap<>();
        campo.put("type", tipo);
        campo.put("description", descricao);
        campo.put("obrigatorio", obrigatorio);
        return campo;
    }
}
