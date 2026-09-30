package sgs_barber.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sessao de conversa entre um cliente/usuario e os agentes de IA.
 * O `usuarioId` vem do front-end e amarra o historico da conversa.
 */
@Entity
@Table(name = "conversas", indexes = {
        @Index(name = "idx_conversa_usuario", columnList = "usuario_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private String usuarioId;

    /** Ultimo agente que respondeu; usado como padrao do proximo turno. */
    @Enumerated(EnumType.STRING)
    @Column(name = "agente_atual", nullable = false)
    private TipoAgente agenteAtual = TipoAgente.AGENDADOR;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm = LocalDateTime.now();

    @PrePersist
    void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadaEm = agora;
        this.atualizadaEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadaEm = LocalDateTime.now();
    }
}
