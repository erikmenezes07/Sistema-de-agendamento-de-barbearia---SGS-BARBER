package sgs_barber.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServicoDTO {
    private Long id;

    @NotBlank(message = "O nome do serviço é obrigatório")
    private String nome;

    @NotNull(message = "O preço é obrigatório")
    @Min(value = 0, message = "O preço deve ser maior ou igual a zero")
    private BigDecimal preco;

    @NotNull(message = "A duração é obrigatória")
    @Min(value = 1, message = "A duração mínima deve ser de 1 minuto")
    private Integer duracaoMinutos;
}