package sgs_barber.ia.agente;

import org.springframework.stereotype.Component;
import sgs_barber.ia.util.Texto;
import sgs_barber.model.TipoAgente;

/**
 * Decide qual agente responde ao turno.
 *
 * Ordem de resolucao:
 *  1. destino explicito no corpo da requisicao (campo `agente_destino`);
 *  2. ultima mensagem do historico, para manter o contexto (o cliente nao
 *     precisa repetir "quero haircut" a cada mensagem);
 *  3. heuristica de palavras-chave, que nao custa nada e nao depende de API.
 *
 * A heuristica existe para que o projeto funcione mesmo sem chave de IA
 * configurada — e porque, para um vocabulario de barbearia tao pequeno, ela
 * acerta mais do que gastaria uma chamada extra ao modelo.
 */
@Component
public class RoteadorAgente {

    /** Palavras que unmistakavelmente pertencem ao Recomendador. */
    private static final String[] CHAVES_RECOMENDADOR = {
            "recomend", "sugest", "sugeri", "dica", "dicas", "estilo", "modelagem", "modelar",
            "combina", "ficaria", "ficava bem", "qual corte", "que corte", "o que fazer",
            "especialista", "especialidade", "quem faz", "melhor para", "kits", "kit",
            "pacote", "combo", "quanto custa", "qual o preco", "valor de", "promocao", "economizar"
    };

    /** Palavras que unmistakavelmente pertencem ao Agendador. */
    private static final String[] CHAVES_AGENDADOR = {
            "agendar", "agendamento", "marcar", "reservar", "reserva", "horario", "vaga",
            "disponibilidade", "disponivel", "livre", "ocupado", "cancelar", "remarcar",
            "trocar", "mudar", "confirmar", "minha agenda", "tenho marcado", "encaixe"
    };

    public TipoAgente resolver(String mensagem, TipoAgente preferido, TipoAgente anterior) {
        if (preferido != null) {
            return preferido;
        }
        if (anterior != null) {
            return anterior;
        }
        return classificar(mensagem);
    }

    /** Heuristica pura, usada tambem como fallback quando nao ha modelo. */
    public TipoAgente classificar(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            return TipoAgente.AGENDADOR;
        }

        int pontosRecomendador = 0;
        for (String chave : CHAVES_RECOMENDADOR) {
            if (Texto.contem(mensagem, chave)) {
                pontosRecomendador++;
            }
        }

        int pontosAgendador = 0;
        for (String chave : CHAVES_AGENDADOR) {
            if (Texto.contem(mensagem, chave)) {
                pontosAgendador++;
            }
        }

        if (pontosRecomendador > pontosAgendador) {
            return TipoAgente.RECOMENDADOR;
        }
        return TipoAgente.AGENDADOR;
    }

    /** Converte "agendamento" / "recomendador" para o enum, tolerando caixa e acento. */
    public TipoAgente doTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String normalizado = Texto.normalizar(texto);
        if (normalizado.contains("recomend")) {
            return TipoAgente.RECOMENDADOR;
        }
        if (normalizado.contains("agend")) {
            return TipoAgente.AGENDADOR;
        }
        return null;
    }
}
