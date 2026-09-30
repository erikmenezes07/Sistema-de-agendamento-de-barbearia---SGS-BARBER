package sgs_barber.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * Retorno completo usado pelos agentes de IA. Traz contexto suficiente para o
 * modelo responder sem precisar de uma segunda chamada: turno do dia, pausa de
 * almoco e todos os slots livres ja filtrados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadeDetalhadaDTO {

    private Long barbeiroId;
    private String barbeiroNome;

    /** Data consultada no formato ISO (yyyy-MM-dd). */
    private String data;

    /** Dia da semana em portugues, para o agente citar "sexta-feira". */
    private String diaSemana;

    /** Duracao total considerada (soma de todos os servicos do combo). */
    private Integer duracaoMinutos;

    /** O barbeiro trabalha neste dia da semana? */
    private boolean atendeNesteDia;

    /** Esta hora especifica esta livre? */
    private boolean disponivel;

    private String motivo;

    /** Turnos configurados no dia (ex: 09:00 as 18:00). */
    private List<String> turnos;

    /** Pausa de almoco, se existir. */
    private String horarioAlmoco;

    /** Todos os horarios livres do dia, ja considerando almoco e conflitos. */
    private List<LocalTime> slotsLivres;

    /** Total de slots livres, para o agente saber se vale oferecer alternativas. */
    private Integer totalSlotsLivres;
}
