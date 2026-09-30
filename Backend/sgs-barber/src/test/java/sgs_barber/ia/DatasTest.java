package sgs_barber.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sgs_barber.ia.util.Datas;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre o QUE o modelo diz, nao o que ele quer dizer: as tools recebem datas e
 * horas em portugues e precisam transformar em {@link LocalDate}/{@link LocalTime}
 * sem quebrar a conversa.
 */
class DatasTest {

    // ------------------------------------------------------------------ datas

    @Test
    @DisplayName("hoje/amanha/ontem sao relativos a data corrente")
    void resolveRelativos() {
        LocalDate hoje = LocalDate.now();
        assertEquals(hoje, Datas.resolverData("quero para hoje"));
        assertEquals(hoje.plusDays(1), Datas.resolverData("amanha de manha"));
        assertEquals(hoje.plusDays(2), Datas.resolverData("depois de amanha"));
        assertEquals(hoje.minusDays(1), Datas.resolverData("ontem"));
    }

    @Test
    @DisplayName("formato ISO e brasileiro")
    void resolveFormatosExplicitos() {
        assertEquals(LocalDate.of(2026, 10, 5), Datas.resolverData("2026-10-05"));
        assertEquals(LocalDate.of(2026, 10, 5), Datas.resolverData("05/10/2026"));
        assertEquals(LocalDate.of(2026, 10, 5), Datas.resolverData("5-10-26"));
    }

    @Test
    @DisplayName("dia da semana em portugues vira a data mais proxima")
    void resolveDiaDaSemana() {
        // As constantes de DayOfWeek sao em ingles: o codigo precisa traduzir,
        // senao DayOfWeek.valueOf("SEXTA") lanca IllegalArgumentException.
        assertEquals(proxima(DayOfWeek.MONDAY), Datas.resolverData("segunda"));
        assertEquals(proxima(DayOfWeek.TUESDAY), Datas.resolverData("terca"));
        assertEquals(proxima(DayOfWeek.WEDNESDAY), Datas.resolverData("quarta-feira"));
        assertEquals(proxima(DayOfWeek.THURSDAY), Datas.resolverData("quinta"));
        assertEquals(proxima(DayOfWeek.FRIDAY), Datas.resolverData("sexta"));
        assertEquals(proxima(DayOfWeek.SATURDAY), Datas.resolverData("sabado"));
        assertEquals(proxima(DayOfWeek.SUNDAY), Datas.resolverData("domingo"));
    }

    @Test
    @DisplayName("texto sem data reconhecivel devolve null e o chamador decide")
    void resolveSemData() {
        assertNull(Datas.resolverData("quero um corte"));
        assertNull(Datas.resolverData(""));
        assertNull(Datas.resolverData(null));
    }

    // ------------------------------------------------------------------ horas

    @Test
    @DisplayName("hora em 14h, 14:30, 14h30 e 14 horas")
    void resolveHora() {
        assertEquals(LocalTime.of(14, 0), Datas.resolverHora("as 14h"));
        assertEquals(LocalTime.of(14, 30), Datas.resolverHora("14:30"));
        assertEquals(LocalTime.of(14, 30), Datas.resolverHora("14h30"));
        assertEquals(LocalTime.of(14, 0), Datas.resolverHora("14 horas"));
        assertEquals(LocalTime.of(9, 15), Datas.resolverHora("9:15 da manha"));
    }

    @Test
    @DisplayName("hora invalida ou ausente devolve null em vez de estourar")
    void resolveHoraInvalida() {
        assertNull(Datas.resolverHora("25h"));
        assertNull(Datas.resolverHora("sem hora aqui"));
        assertNull(Datas.resolverHora(null));
    }

    // ---------------------------------------------------------------- janelas

    @Test
    @DisplayName("manha, tarde e noite viram janelas; sem periodo, null")
    void janelas() {
        assertEquals(LocalTime.of(12, 0), Datas.janelaDoDia("a tarde")[0]);
        assertEquals(LocalTime.of(18, 0), Datas.janelaDoDia("tarde")[1]);
        assertEquals(LocalTime.of(5, 0), Datas.janelaDoDia("de manha")[0]);
        assertEquals(LocalTime.of(18, 0), Datas.janelaDoDia("a noite")[0]);
        assertNull(Datas.janelaDoDia(null));
        assertNull(Datas.janelaDoDia(""));
    }

    @Test
    @DisplayName("janela e inclusiva nas bordas")
    void janelaInclusiva() {
        LocalTime[] janela = Datas.janelaDoDia("tarde");
        assertTrue(Datas.dentroDaJanela(LocalTime.of(12, 0), janela[0], janela[1]));
        assertTrue(Datas.dentroDaJanela(LocalTime.of(18, 0), janela[0], janela[1]));
        assertTrue(!Datas.dentroDaJanela(LocalTime.of(11, 45), janela[0], janela[1]));
        assertTrue(!Datas.dentroDaJanela(LocalTime.of(18, 15), janela[0], janela[1]));
    }

    // ------------------------------------------------------- regressao: "amanha"

    /**
     * "amanha" contem a sequencia "manha". Com {@code contains} simples,
     * "amanha de tarde" era classificado como manha e o cliente perdia a
     * janela que pediu. Precisa continuar assim mesmo se o texto mudar.
     */
    @Test
    @DisplayName("'amanha' nao pode ser lido como 'manha'")
    void amanhaNaoViraManha() {
        assertEquals(12, Datas.janelaDoDia("amanha de tarde")[0].getHour());
        assertEquals(18, Datas.janelaDoDia("amanha de tarde")[1].getHour());
        assertEquals(5, Datas.janelaDoDia("amanha de manha")[0].getHour());
        assertEquals(12, Datas.janelaDoDia("amanha de manha")[1].getHour());
    }

    @Test
    @DisplayName("periodo continua funcionando com e sem data junto")
    void periodoComDataNoTexto() {
        assertEquals(12, Datas.janelaDoDia("voce tem horario amanha a tarde?")[0].getHour());
        assertEquals(18, Datas.janelaDoDia("so de noite")[0].getHour());
        assertNull(Datas.janelaDoDia("amanha, qualquer hora"));
    }

    @Test
    @DisplayName("hora pedida e lida antes do periodo, sem conflitar")
    void horaEPPeriodoSeparados() {
        assertEquals(LocalTime.of(14, 0), Datas.resolverHora("amanha as 14h"));
        assertNull(Datas.resolverHora("amanha de tarde"));
        assertEquals(12, Datas.janelaDoDia("amanha as 14h de tarde")[0].getHour());
    }

    private static LocalDate proxima(DayOfWeek alvo) {
        LocalDate hoje = LocalDate.now();
        int delta = alvo.getValue() - hoje.getDayOfWeek().getValue();
        if (delta < 0) {
            delta += 7;
        }
        return hoje.plusDays(delta);
    }
}
