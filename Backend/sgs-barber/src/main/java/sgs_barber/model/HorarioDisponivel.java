package sgs_barber.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "horarios_disponiveis", uniqueConstraints = @UniqueConstraint(
        name = "uk_horario_barbeiro_data_inicio",
        columnNames = {"barbeiro_id", "data", "hora_inicio"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioDisponivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(nullable = false)
    private Boolean disponivel = true;
}