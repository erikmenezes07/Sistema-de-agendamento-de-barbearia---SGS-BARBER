package sgs_barber.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sgs_barber.ia.ferramenta.Esquema;
import sgs_barber.ia.ferramenta.Ferramenta;
import sgs_barber.ia.ferramenta.RegistroFerramentas;
import sgs_barber.model.TipoAgente;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O registro e o que materializa a separacao de responsabilidade: cada tool
 * declara quem pode chama-la. Se essa checagem ceder, o Recomendador passa a
 * poder cancelar agendamento, que e exatamente o que a arquitetura proibe.
 */
class RegistroFerramentasTest {

    private static final String CRIAR = "criar_agendamento";
    private static final String CANCELAR = "cancelar_agendamento";
    private static final String LISTAR_SERVICOS = "listar_servicos";
    private static final String ENCAMINHAR = "encaminhar_para_agente";

    private RegistroFerramentas registroComFake() {
        RegistroFerramentas registro = new RegistroFerramentas(new ObjectMapper());

        registro.add(new Ferramenta(CRIAR, "cria", null,
                Set.of(TipoAgente.AGENDADOR), args -> "criado"));
        registro.add(new Ferramenta(CANCELAR, "cancela", null,
                Set.of(TipoAgente.AGENDADOR), args -> "cancelado"));
        registro.add(new Ferramenta(LISTAR_SERVICOS, "lista", null,
                Set.of(TipoAgente.AGENDADOR, TipoAgente.RECOMENDADOR), args -> "[]"));
        registro.add(new Ferramenta(ENCAMINHAR, "encaminha", null,
                Set.of(TipoAgente.AGENDADOR, TipoAgente.RECOMENDADOR), args -> "ok"));
        return registro;
    }

    @Test
    @DisplayName("so o Agendador enxerga as tools que escrevem na agenda")
    void escritaEExclusivaDoAgendador() {
        RegistroFerramentas registro = registroComFake();

        assertTrue(registro.pertenceAoAgente(CRIAR, TipoAgente.AGENDADOR));
        assertTrue(registro.pertenceAoAgente(CANCELAR, TipoAgente.AGENDADOR));
        assertFalse(registro.pertenceAoAgente(CRIAR, TipoAgente.RECOMENDADOR));
        assertFalse(registro.pertenceAoAgente(CANCELAR, TipoAgente.RECOMENDADOR));

        assertTrue(registro.nomesDisponiveis(TipoAgente.RECOMENDADOR).contains(LISTAR_SERVICOS));
        assertFalse(registro.nomesDisponiveis(TipoAgente.RECOMENDADOR).contains(CRIAR));
    }

    @Test
    @DisplayName("os dois agentes conseguem ler servicos e se encaminhar")
    void leituraComumAosDois() {
        RegistroFerramentas registro = registroComFake();

        for (TipoAgente agente : List.of(TipoAgente.AGENDADOR, TipoAgente.RECOMENDADOR)) {
            assertTrue(registro.pertenceAoAgente(LISTAR_SERVICOS, agente));
            assertTrue(registro.pertenceAoAgente(ENCAMINHAR, agente));
        }
    }

    @Test
    @DisplayName("chamar tool sem agente informado nao estoura NullPointerException")
    void executarSemAgenteNaoQuebra() {
        RegistroFerramentas registro = registroComFake();

        // Set.of(...) lanca NPE em contains(null); a guarda precisa existir para
        // que um turno sem agente resolvido nao derrube a requisição com 500.
        String resposta = assertDoesNotThrow(() -> registro.executar(LISTAR_SERVICOS, Map.of()));
        assertTrue(resposta.contains("erro"), resposta);
    }

    @Test
    @DisplayName("erro de regra de negocio volta como JSON para o modelo, nao como excecao")
    void erroDeNegocioViraFeedback() {
        RegistroFerramentas registro = new RegistroFerramentas(new ObjectMapper());
        registro.add(new Ferramenta("quebra", "quebra", null, Set.of(TipoAgente.AGENDADOR),
                args -> {
                    throw new ConversaAgenteException("barbeiro 99 nao existe");
                }));

        String resposta = registro.executar("quebra", Map.of(), TipoAgente.AGENDADOR);
        assertTrue(resposta.contains("barbeiro 99 nao existe"), resposta);
    }

    @Test
    @DisplayName("tool inexistente responde com erro em vez de 500")
    void toolInexistente() {
        RegistroFerramentas registro = registroComFake();

        String resposta = registro.executar("nao_existe", Map.of(), TipoAgente.AGENDADOR);
        assertTrue(resposta.contains("nao_existe"), resposta);
    }

    @Test
    @DisplayName("tool duplicada quebra o boot em vez de sobrescrever silenciosamente")
    void toolDuplicada() {
        RegistroFerramentas registro = registroComFake();

        Ferramenta duplicada = new Ferramenta(CRIAR, "outra", null,
                Set.of(TipoAgente.AGENDADOR), args -> "x");
        assertThrows(IllegalStateException.class, () -> registro.add(duplicada));
    }

    @Test
    @DisplayName("argumentos null viram objeto vazio em vez de NPE")
    void argumentosNulos() {
        RegistroFerramentas registro = new RegistroFerramentas(new ObjectMapper());
        registro.add(new Ferramenta("eco", "eco", null, Set.of(TipoAgente.AGENDADOR),
                args -> "recebi " + args.size()));

        assertTrue(registro.executar("eco", null, TipoAgente.AGENDADOR).contains("recebi 0"));
    }

    @Test
    @DisplayName("o schema enviado ao modelo tem o formato de function calling")
    void schemaNoFormatoEsperado() {
        Ferramenta ferramenta = new Ferramenta("listar_servicos", "lista os servicos",
                null, Set.of(TipoAgente.AGENDADOR), args -> "[]");

        Map<String, Object> schema = ferramenta.paraSchema();
        assertEquals("function", schema.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> funcao = (Map<String, Object>) schema.get("function");
        assertEquals("listar_servicos", funcao.get("name"));
        assertEquals("lista os servicos", funcao.get("description"));

        @SuppressWarnings("unchecked")
        Map<String, Object> parametros = (Map<String, Object>) funcao.get("parameters");
        assertEquals("object", parametros.get("type"));
        assertEquals(Map.of(), parametros.get("properties"));
    }

    // ----------------------------------------------------------------- esquema

    @Test
    @DisplayName("Esquema monta JSON Schema valido e tira a chave obrigatorio do miolo")
    void esquemaValido() {
        Map<String, Object> schema = Esquema.objeto(
                Esquema.inteiro("barbeiro_id", "id do barbeiro", true),
                Esquema.inteiro("duracao", "minutos", false),
                Esquema.string("periodo", "manha/tarde/noite", false),
                Esquema.booleano("confirmar", "confirma?", true));

        assertEquals("object", schema.get("type"));
        assertEquals(List.of("barbeiro_id", "confirmar"), schema.get("required"));

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        assertEquals(4, props.size());

        for (Map.Entry<String, Object> campo : props.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> def = (Map<String, Object>) campo.getValue();
            assertTrue(def.containsKey("type"), campo.getKey());
            assertTrue(def.containsKey("description"), campo.getKey());
            // "obrigatorio" e marcador interno: nao pode vazar para o modelo.
            assertFalse(def.containsKey("obrigatorio"), campo.getKey());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> confirmar = (Map<String, Object>) props.get("confirmar");
        assertEquals("boolean", confirmar.get("type"));
    }

    @Test
    @DisplayName("Esquema de opcao restringe o modelo aos valores permitidos")
    void esquemaDeOpcao() {
        Map<String, Object> schema = Esquema.objeto(
                Esquema.opcao("agente", "destino", true, "agendador", "recomendador"),
                Esquema.listaDeInteiros("servicos_ids", "ids", false),
                Esquema.numero("fator", "multiplicador", false));

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");

        @SuppressWarnings("unchecked")
        Map<String, Object> agente = (Map<String, Object>) props.get("agente");
        assertEquals(List.of("agendador", "recomendador"), agente.get("enum"));

        @SuppressWarnings("unchecked")
        Map<String, Object> servicos = (Map<String, Object>) props.get("servicos_ids");
        assertEquals("array", servicos.get("type"));
        assertEquals(Map.of("type", "integer"), servicos.get("items"));

        @SuppressWarnings("unchecked")
        Map<String, Object> fator = (Map<String, Object>) props.get("fator");
        assertEquals("number", fator.get("type"));

        // So o primeiro campo era obrigatorio.
        assertEquals(List.of("agente"), schema.get("required"));
    }
}
