package sgs_barber.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Corpo do POST /api/chat.
 *
 * O front-end nunca fala com a API da IA: manda a mensagem e o id do usuario
 * para o back-end, que decide o agente, executa as tools e formata a resposta.
 */
@Data
public class ChatRequestDTO {

    @NotBlank(message = "O usuario_id é obrigatório")
    private String usuarioId;

    @NotBlank(message = "A mensagem é obrigatória")
    private String mensagem;

    /**
     * Destino forcado: "agendamento" ou "recomendacao".
     * Se ausente, o back-end classifica a mensagem.
     */
    private String agenteDestino;

    /** Opcional: associa a conversa a um cliente ja cadastrado. */
    private Long clienteId;

    /** Se true, limpa o historico antes de responder. */
    private Boolean reiniciar = false;
}
