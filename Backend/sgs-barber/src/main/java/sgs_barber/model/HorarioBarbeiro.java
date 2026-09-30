package sgs_barber.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "horarios_barbeiro")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioBarbeiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    // Segue java.time.DayOfWeek.getValue(): 1 = SEGUNDA ... 7 = DOMINGO
    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    /**
     * Intervalo de almoço/descanso dentro do turno. Fica gravado em duas colunas
     * (inicio e fim) porque um unico campo "horario_almoco" nao permite fazer a
     * conta de sobreposicao de intervalos, que e o que o calculo de disponibilidade
     * do Agente 1 precisa.
     */
    @Column(name = "almoco_inicio")
    private LocalTime almocoInicio;

    @Column(name = "almoco_fim")
    private LocalTime almocoFim;

    /** Turno sem pausa = o barbeiro atende em tempo continuo. */
    public boolean isSemAlmoco() {
        return almocoInicio == null || almocoFim == null;
    }
}
