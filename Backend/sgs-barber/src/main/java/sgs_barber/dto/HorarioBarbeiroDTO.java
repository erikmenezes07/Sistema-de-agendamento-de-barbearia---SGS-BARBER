package sgs_barber.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class HorarioBarbeiroDTO {
    private Long id;

    @NotNull(message = "O barbeiro é obrigatório")
    private Long barbeiroId;

    @NotNull(message = "O dia da semana é obrigatório")
    @Min(value = 1, message = "Dia da semana deve ser entre 1 (segunda) e 7 (domingo)")
    @Max(value = 7, message = "Dia da semana deve ser entre 1 (segunda) e 7 (domingo)")
    private Integer diaSemana;

    @NotNull(message = "A hora de início é obrigatória")
    private LocalTime horaInicio;

    @NotNull(message = "A hora de fim é obrigatória")
    private LocalTime horaFim;

    /** Início do intervalo de almoço/descanso. Opcional. */
    private LocalTime almocoInicio;

    /** Fim do intervalo de almoço/descanso. Opcional. */
    private LocalTime almocoFim;
}
