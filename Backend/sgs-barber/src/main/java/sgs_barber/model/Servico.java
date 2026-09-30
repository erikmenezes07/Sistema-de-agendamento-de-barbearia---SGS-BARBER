package sgs_barber.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "servicos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    /**
     * Texto lido pelo Agente Recomendador para sugerir o servico adequado ao
     * pedido do cliente ("quero o cabelo mais curto", "so a barba").
     */
    @Column(columnDefinition = "text")
    private String descricao;

    @Column(nullable = false)
    private BigDecimal preco;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    /** Marcado como TRUE para kits promotionais (ex: "Corte + Barba"). */
    @Column(name = "combo", nullable = false)
    private Boolean combo = false;
}
