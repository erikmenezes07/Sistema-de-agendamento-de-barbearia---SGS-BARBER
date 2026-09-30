package sgs_barber.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** Resposta do POST /api/chat. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {

    private Long conversaId;

    /** Texto final em linguagem natural para o cliente. */
    private String mensagem;

    /** AGENDADOR ou RECOMENDADOR. */
    private String agente;

    private String agenteNome;

    /**
     * Trace do que o agente consultou de verdade. Alem deuxiliar a depuracao,
     * e o que permite ao front-end mostrar ao usuario quais dados vieram do
     * banco (e nao da geracao do modelo).
     */
    private List<Map<String, Object>> ferramentasUsadas;

    /** True quando houve passagem de um agente para o outro. */
    private boolean encaminhou;

    /** Aviso operacional (ex.: rodando sem chave de IA). */
    private String aviso;
}
