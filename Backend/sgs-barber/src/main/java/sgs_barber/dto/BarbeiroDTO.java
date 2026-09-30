package sgs_barber.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class BarbeiroDTO {
    private Long id;

    @NotBlank(message = "O nome do barbeiro é obrigatório")
    private String nome;

    @NotBlank(message = "O telefone é obrigatório")
    private String telefone;

    private String fotoUrl;

    /** Ex.: ["Degradê", "Barba"]. O salvamento normaliza para uma coluna VARCHAR. */
    private List<String> especialidades;

    private Boolean ativo = true;
}
