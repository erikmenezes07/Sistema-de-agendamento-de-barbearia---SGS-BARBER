package sgs_barber.ia.ferramenta;

import org.springframework.stereotype.Component;
import sgs_barber.ia.util.Argumentos;
import sgs_barber.ia.util.Texto;
import sgs_barber.model.Barbeiro;
import sgs_barber.model.TipoAgente;
import sgs_barber.repository.BarbeiroRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static sgs_barber.ia.ferramenta.Esquema.objeto;
import static sgs_barber.ia.ferramenta.Esquema.string;

/**
 * Tools de leitura da equipe.
 *
 * `consultar_barbeiros` é compartilhada pelos dois agentes: o Agendador precisa
 * do id para checar agenda, o Recomendador precisa da especialidade para
 * indicar quem faz o trabalho.
 */
@Component
public class FerramentasBarbeiro implements ConjuntoDeFerramentas {

    public static final String CONSULTAR_BARBEIROS = "consultar_barbeiros";
    public static final String LOCALIZAR_BARBEIRO = "localizar_barbeiro";

    private final BarbeiroRepository repository;

    public FerramentasBarbeiro(BarbeiroRepository repository) {
        this.repository = repository;
    }

    @Override
    public void registrar(RegistroFerramentas registro) {
        registro.add(new Ferramenta(
                CONSULTAR_BARBEIROS,
                "Lista os barbeiros ativos com nome, foto, telefone e especialidades. "
                        + "Use para descobrir o id do barbeiro antes de checar a agenda.",
                objeto(string("especialidade",
                        "Filtro opcional por especialidade ou nome. Ex.: 'barba', 'degrade', 'tesoura'.")),
                Set.of(TipoAgente.AGENDADOR, TipoAgente.RECOMENDADOR),
                this::consultar));

        registro.add(new Ferramenta(
                LOCALIZAR_BARBEIRO,
                "Descobre o id exato de um barbeiro a partir do nome informado pelo cliente. "
                        + "Use quando o cliente citar um nome e for preciso confirmar se existe.",
                objeto(string("nome", "Nome do barbeiro citado pelo cliente.", true)),
                Set.of(TipoAgente.AGENDADOR, TipoAgente.RECOMENDADOR),
                this::localizar));
    }

    private Object consultar(Map<String, Object> args) {
        String filtro = Argumentos.texto(args, "especialidade");
        List<Barbeiro> barbeiros = filtro.isBlank()
                ? repository.findByAtivoTrueOrderByNomeAsc()
                : repository.buscarPorEspecialidade(filtro);

        // A busca por SQL nao remove acento; refaz o filtro em memoria para nao
        // devolver barbeiro que nao tem a especialidade pedida.
        if (!filtro.isBlank()) {
            String alvo = Texto.normalizar(filtro);
            List<Barbeiro> filtrados = new ArrayList<>();
            for (Barbeiro b : barbeiros) {
                boolean bate = Texto.normalizar(b.getNome()).contains(alvo)
                        || b.getEspecialidades().stream().anyMatch(e -> Texto.normalizar(e).contains(alvo));
                if (bate) {
                    filtrados.add(b);
                }
            }
            barbeiros = filtrados;
        }

        List<Map<String, Object>> resposta = new ArrayList<>();
        for (Barbeiro b : barbeiros) {
            resposta.add(paraMapa(b));
        }
        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("quantidade", resposta.size());
        saida.put("barbeiros", resposta);
        if (resposta.isEmpty()) {
            saida.put("aviso", filtro.isBlank()
                    ? "Nenhum barbeiro cadastrado."
                    : "Nenhum barbeiro encontrado para '" + filtro + "'.");
        }
        return saida;
    }

    private Object localizar(Map<String, Object> args) {
        String nome = Argumentos.texto(args, "nome");
        List<Barbeiro> encontrados = repository.findByNomeContainingIgnoreCase(nome);
        List<Map<String, Object>> resposta = new ArrayList<>();
        for (Barbeiro b : encontrados) {
            resposta.add(paraMapa(b));
        }
        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("quantidade", resposta.size());
        saida.put("barbeiros", resposta);
        if (resposta.isEmpty()) {
            saida.put("aviso", "Nenhum barbeiro chamado '" + nome + "' foi encontrado. "
                    + "Consulte a lista completa antes de afirmar que ele nao existe.");
        }
        return saida;
    }

    private Map<String, Object> paraMapa(Barbeiro b) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", b.getId());
        mapa.put("nome", b.getNome());
        mapa.put("telefone", b.getTelefone());
        mapa.put("foto_url", b.getFotoUrl());
        mapa.put("especialidades", b.getEspecialidades());
        mapa.put("ativo", b.getAtivo());
        return mapa;
    }
}
