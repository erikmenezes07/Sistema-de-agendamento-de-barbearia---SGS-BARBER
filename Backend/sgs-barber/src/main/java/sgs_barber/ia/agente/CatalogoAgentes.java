package sgs_barber.ia.agente;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import sgs_barber.ia.ferramenta.FerramentasAgendamento;
import sgs_barber.ia.ferramenta.FerramentasBarbeiro;
import sgs_barber.ia.ferramenta.FerramentasCliente;
import sgs_barber.ia.ferramenta.FerramentasServico;
import sgs_barber.model.TipoAgente;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Carrega os prompts de sistema do classpath e monta a definicao de cada
 * agente. Os prompts vivem em {@code src/main/resources/prompts/*.md} para
 * poderem ser revisados sem compilar codigo Java.
 */
@Component
public class CatalogoAgentes {

    public static final String PROMPT_AGENDADOR = "prompts/agendador.md";
    public static final String PROMPT_RECOMENDADOR = "prompts/recomendador.md";

    private final Map<TipoAgente, Agente> agentes = new EnumMap<>(TipoAgente.class);

    public CatalogoAgentes() {
        // As ferramentas de escrita ficam so no Agendador; o Recomendador recebe
        // apenas leitura de catalogo, e por isso nao consegue gravar nada.
        this.agentes.put(TipoAgente.AGENDADOR, new Agente(
                TipoAgente.AGENDADOR,
                "Agendador",
                carregar(PROMPT_AGENDADOR),
                Set.of(
                        FerramentasBarbeiro.CONSULTAR_BARBEIROS,
                        FerramentasBarbeiro.LOCALIZAR_BARBEIRO,
                        FerramentasServico.LISTAR_SERVICOS,
                        FerramentasServico.OBTER_TEMPO_SERVICO,
                        FerramentasServico.CALCULAR_COMBO,
                        FerramentasCliente.BUSCAR_CLIENTE,
                        FerramentasAgendamento.VERIFICAR_DISPONIBILIDADE,
                        FerramentasAgendamento.CRIAR_AGENDAMENTO,
                        FerramentasAgendamento.LISTAR_AGENDAMENTOS,
                        FerramentasAgendamento.CANCELAR_AGENDAMENTO,
                        FerramentasAgendamento.ENCAMINHAR)));

        this.agentes.put(TipoAgente.RECOMENDADOR, new Agente(
                TipoAgente.RECOMENDADOR,
                "Recomendador",
                carregar(PROMPT_RECOMENDADOR),
                Set.of(
                        FerramentasBarbeiro.CONSULTAR_BARBEIROS,
                        FerramentasBarbeiro.LOCALIZAR_BARBEIRO,
                        FerramentasServico.LISTAR_SERVICOS,
                        FerramentasServico.OBTER_TEMPO_SERVICO,
                        FerramentasServico.CALCULAR_COMBO,
                        FerramentasAgendamento.ENCAMINHAR)));
    }

    public Agente obter(TipoAgente tipo) {
        Agente agente = agentes.get(tipo);
        if (agente == null) {
            throw new IllegalArgumentException("Agente desconhecido: " + tipo);
        }
        return agente;
    }

    public String nomeExibicao(TipoAgente tipo) {
        return obter(tipo).nomeExibicao();
    }

    private String carregar(String caminho) {
        try (var entrada = new ClassPathResource(caminho).getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Prompt nao encontrado no classpath: " + caminho, e);
        }
    }
}
