package sgs_barber.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String telefone;

    // Login do cliente, usado pelo UsuarioBackfillService para criar a conta a
    // partir de um cliente ja cadastrado. Este e o lado dono da relacao, por
    // isso a coluna usuario_id fica aqui.
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;
}
