package sgs_barber.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import sgs_barber.model.StatusAgendamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class AgendamentoDTO {
    private Long id;

    @NotNull(message = "O cliente é obrigatório")
    private Long clienteId;

    @NotNull(message = "O barbeiro é obrigatório")
    private Long barbeiroId;

    @NotNull(message = "O serviço é obrigatório")
    private Long servicoId;

    // Nomes preenchidos na resposta. Facilitam a vida do agente de IA, que
    // consegue dizer "sexta às 14h com o Daniel" sem consultar outras tools.
    private String clienteNome;
    private String barbeiroNome;
    private String servicoNome;

    @NotNull(message = "A data do agendamento é obrigatória")
    private LocalDate dataAgendamento;

    @NotNull(message = "A hora de início é obrigatória")
    private LocalTime horaInicio;

    // Calculado automaticamente a partir da duração do serviço; não precisa vir no request
    private LocalTime horaFim;

    /** Duração total ocupada na agenda (soma dos serviços, no caso de combo). */
    public int getDuracaoMinutos() {
        if (horaInicio == null || horaFim == null) {
            return 0;
        }
        return (int) java.time.Duration.between(horaInicio, horaFim).toMinutes();
    }

    private StatusAgendamento status;

    private String observacao;

    private LocalDateTime dataCriacao;
}
