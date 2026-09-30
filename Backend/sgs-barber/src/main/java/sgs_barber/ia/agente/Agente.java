package sgs_barber.ia.agente;

import sgs_barber.model.TipoAgente;

import java.util.Set;

/**
 * Definição de um agente: identidade, prompt de sistema e quais ferramentas ele
 * pode usar.
 *
 * A separação em dois agentes é o que mantém cada conversa com um objetivo
 * único. O Recomendador é cheaply especializado em catálogo e não enxerga
 * agenda; o Agendador enxerga agenda e não "opina" sobre estilo.
 */
public record Agente(
        TipoAgente tipo,
        String nomeExibicao,
        String promptBase,
        Set<String> ferramentas
) {

    public String montarPrompt(String contexto) {
        if (contexto == null || contexto.isBlank()) {
            return promptBase.replace("{{contexto}}", "- (sem contexto adicional nesta sessao)");
        }
        return promptBase.replace("{{contexto}}", contexto);
    }
}
