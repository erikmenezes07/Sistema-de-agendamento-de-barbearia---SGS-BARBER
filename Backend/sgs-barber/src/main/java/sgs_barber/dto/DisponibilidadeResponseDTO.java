package sgs_barber.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadeResponseDTO {

    private boolean disponivel;

    // Explica o motivo quando disponivel = false (ex: "fora do horário de expediente",
    // "barbeiro não atende neste dia", "horário já ocupado")
    private String motivo;

    // Sugestões de horários livres naquele mesmo dia, para o agente de IA oferecer
    // como alternativa quando o horário pedido não estiver disponível
    private List<LocalTime> horariosAlternativos;
}
