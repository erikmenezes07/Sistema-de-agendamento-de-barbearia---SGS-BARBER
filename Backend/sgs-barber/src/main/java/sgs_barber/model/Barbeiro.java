package sgs_barber.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "barbeiros")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Barbeiro {

    /**
     * Separador usado na coluna `especialidades` (VARCHAR). Mantemos tudo em uma
     * única coluna para facilitar a busca por texto feito pelo Agente Recomendador
     * ("quem faz barba?") sem precisar de tabela de junção.
     */
    public static final String SEPARADOR_ESPECIALIDADES = ",";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String telefone;

    // Foto usada pelo front-end e pelo card que o Agente 1 descreve ao cliente
    @Column(name = "foto_url")
    private String fotoUrl;

    /**
     * Ex.: "Degradê,Barba,Tesoura". Exposto como lista por {@link #getEspecialidades()}
     * e gravado por {@link #setEspecialidades(List)}.
     */
    @Column(name = "especialidades")
    private String especialidades;

    @Column(nullable = false)
    private Boolean ativo = true;

    // Login do barbeiro, usado pelo UsuarioBackfillService para criar a conta a
    // partir de um barbeiro ja cadastrado. Este e o lado dono da relacao, por
    // isso a coluna usuario_id fica aqui.
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    public List<String> getEspecialidades() {
        if (especialidades == null || especialidades.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(especialidades.split(SEPARADOR_ESPECIALIDADES))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    public void setEspecialidades(List<String> lista) {
        if (lista == null || lista.isEmpty()) {
            this.especialidades = null;
            return;
        }
        this.especialidades = lista.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(SEPARADOR_ESPECIALIDADES));
    }
}
