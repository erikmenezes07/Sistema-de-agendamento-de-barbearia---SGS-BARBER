package sgs_barber.model;

/**
 * Agentes de IA do SGS-BARBER. A separacao existe para manter cada prompt
 * pequeno e com uma unica responsabilidade, o que reduz alucinacao e custo:
 *
 * AGENDADOR   -> conversa operacional (consultar agenda, criar/cancelar reserva)
 * RECOMENDADOR -> conversa de estilo/servico (o que cortar, com quem, qual combo)
 */
public enum TipoAgente {
    AGENDADOR,
    RECOMENDADOR
}
