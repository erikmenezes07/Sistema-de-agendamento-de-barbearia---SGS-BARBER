package sgs_barber.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sgs_barber.ia.agente.RoteadorAgente;
import sgs_barber.model.TipoAgente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A separacao entre os dois agentes e a peca central da arquitetura, entao ela
 * precisa ser previsivel mesmo sem modelo de linguagem: com a heuristica, a
 * conversa continua coerente quando a chave da API falta ou quando a
 * pergunta cai no vao entre os dois agentes.
 */
class RoteadorAgenteTest {

    private final RoteadorAgente roteador = new RoteadorAgente();

    @Test
    @DisplayName("pedido de estilo/servico/especialidade vai para o Recomendador")
    void mandaParaRecomendador() {
        assertEquals(TipoAgente.RECOMENDADOR, roteador.classificar("qual corte ficaria bem pra mim?"));
        assertEquals(TipoAgente.RECOMENDADOR, roteador.classificar("quem faz degrade aqui?"));
        assertEquals(TipoAgente.RECOMENDADOR, roteador.classificar("quanto custa o combo?"));
        assertEquals(TipoAgente.RECOMENDADOR, roteador.classificar("me da uma dica de estilo"));
    }

    @Test
    @DisplayName("pedido de horario/reserva/cancelamento vai para o Agendador")
    void mandaParaAgendador() {
        assertEquals(TipoAgente.AGENDADOR, roteador.classificar("tenho horario amanha?"));
        assertEquals(TipoAgente.AGENDADOR, roteador.classificar("quero marcar com o Daniel"));
        assertEquals(TipoAgente.AGENDADOR, roteador.classificar("cancela meu agendamento"));
    }

    @Test
    @DisplayName("frase ambigua ou vazia vai para o Agendador, que tem o escopo maior")
    void desempataParaAgendador() {
        assertEquals(TipoAgente.AGENDADOR, roteador.classificar("ola"));
        assertEquals(TipoAgente.AGENDADOR, roteador.classificar(""));
        assertEquals(TipoAgente.AGENDADOR, roteador.classificar(null));
    }

    @Test
    @DisplayName("destino explicito tem prioridade sobre a heuristica")
    void destinoExplicitoVence() {
        assertEquals(TipoAgente.RECOMENDADOR,
                roteador.resolver("quero agendar", TipoAgente.RECOMENDADOR, null));
        assertEquals(TipoAgente.AGENDADOR,
                roteador.resolver("qual corte", null, TipoAgente.AGENDADOR));
    }

    @Test
    @DisplayName("sem destino e sem anterior, classifica pelo texto")
    void semPistasClassifica() {
        assertEquals(TipoAgente.RECOMENDADOR, roteador.resolver("me sugere um estilo", null, null));
        assertEquals(TipoAgente.AGENDADOR, roteador.resolver("quero reservar", null, null));
    }

    @Test
    @DisplayName("doTexto tolera caixa, acento e texto desconhecido")
    void leDestino() {
        assertEquals(TipoAgente.AGENDADOR, roteador.doTexto("agendador"));
        assertEquals(TipoAgente.AGENDADOR, roteador.doTexto("Agendamento"));
        assertEquals(TipoAgente.RECOMENDADOR, roteador.doTexto("recomendador"));
        assertEquals(TipoAgente.RECOMENDADOR, roteador.doTexto("Recomendação"));
        assertNull(roteador.doTexto("barbeiro"));
        assertNull(roteador.doTexto(null));
    }
}
