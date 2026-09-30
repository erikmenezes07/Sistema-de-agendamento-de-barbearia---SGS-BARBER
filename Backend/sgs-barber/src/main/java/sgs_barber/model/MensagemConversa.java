package sgs_barber.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Uma mensagem do historico. Guarda tambem as chamadas de ferramenta para que
 * seja possivel reconstruir o contexto enviado ao modelo no turno seguinte e,
 * no front-end, exibir um "trace" do que o agente realmente consultou no banco.
 */
@Entity
@Table(name = "mensagens_conversa", indexes = {
        @Index(name = "idx_mensagem_conversa", columnList = "conversa_id, ordem")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensagemConversa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversa_id", nullable = false)
    private Conversa conversa;

    @Column(nullable = false)
    private Integer ordem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PapelMensagem papel;

    @Column(columnDefinition = "text")
    private String conteudo;

    @Enumerated(EnumType.STRING)
    private TipoAgente agente;

    /** Nome da tool executada quando papel = TOOL. */
    @Column(name = "ferramenta_nome")
    private String ferramentaNome;

    /** Argumentos da tool em JSON. */
    @Column(name = "ferramenta_argumentos", columnDefinition = "text")
    private String ferramentaArgumentos;

    /** Retorno da tool em JSON, truncado para nao estourar o contexto. */
    @Column(name = "ferramenta_resultado", columnDefinition = "text")
    private String ferramentaResultado;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    @PrePersist
    void aoCriar() {
        this.criadaEm = LocalDateTime.now();
    }
}
