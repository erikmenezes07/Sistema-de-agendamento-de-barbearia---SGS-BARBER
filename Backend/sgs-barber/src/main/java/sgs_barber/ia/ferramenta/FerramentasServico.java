package sgs_barber.ia.ferramenta;

import org.springframework.stereotype.Component;
import sgs_barber.ia.ConversaAgenteException;
import sgs_barber.ia.util.Argumentos;
import sgs_barber.ia.util.Texto;
import sgs_barber.model.Servico;
import sgs_barber.model.TipoAgente;
import sgs_barber.repository.ServicoRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static sgs_barber.ia.ferramenta.Esquema.objeto;
import static sgs_barber.ia.ferramenta.Esquema.listaDeInteiros;
import static sgs_barber.ia.ferramenta.Esquema.string;

/**
 * Tools de catalogo de servico: o que existe, quanto tempo leva e quanto custa.
 *
 * Sao a base do Agente Recomendador (escolher servico) e do Agendador
 * (descobrir a duracao antes de checar a agenda).
 */
@Component
public class FerramentasServico implements ConjuntoDeFerramentas {

    public static final String LISTAR_SERVICOS = "listar_servicos";
    public static final String OBTER_TEMPO_SERVICO = "obter_tempo_servico";
    public static final String CALCULAR_COMBO = "calcular_combo";

    private final ServicoRepository repository;

    public FerramentasServico(ServicoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void registrar(RegistroFerramentas registro) {
        registro.add(new Ferramenta(
                LISTAR_SERVICOS,
                "Lista os servicos da barbearia com nome, descricao, preco e duracao. "
                        + "Use um termo de busca para filtrar, por exemplo 'barba' ou 'corte'.",
                objeto(string("termo", "Filtro opcional por nome ou descricao do servico.")),
                Set.of(TipoAgente.AGENDADOR, TipoAgente.RECOMENDADOR),
                this::listar));

        registro.add(new Ferramenta(
                OBTER_TEMPO_SERVICO,
                "Retorna a duracao estimada de um servico em minutos. "
                        + "Use antes de verificar a agenda: a soma das duracoes e o tempo que sera reservado.",
                objeto(
                        string("servico_id", "Id do servico.", false),
                        string("servico_nome", "Nome do servico, se o id nao for conhecido.", false)),
                Set.of(TipoAgente.AGENDADOR, TipoAgente.RECOMENDADOR),
                this::tempoServico));

        registro.add(new Ferramenta(
                CALCULAR_COMBO,
                "Soma duracao e preco de varios servicos e devolve o total. "
                        + "Use para montar pacotes como 'corte + barba' antes de agendar.",
                objeto(listaDeInteiros("servico_ids", "Ids dos servicos que fazem parte do combo.", true)),
                Set.of(TipoAgente.AGENDADOR, TipoAgente.RECOMENDADOR),
                this::combo));
    }

    private Object listar(Map<String, Object> args) {
        String termo = Argumentos.texto(args, "termo");
        List<Servico> servicos = termo.isBlank()
                ? repository.findAll()
                : repository.buscarPorTexto(termo);

        if (!termo.isBlank()) {
            String alvo = Texto.normalizar(termo);
            List<Servico> filtrados = new ArrayList<>();
            for (Servico s : servicos) {
                if (Texto.normalizar(s.getNome()).contains(alvo)
                        || Texto.normalizar(s.getDescricao()).contains(alvo)) {
                    filtrados.add(s);
                }
            }
            servicos = filtrados;
        }

        List<Map<String, Object>> resposta = new ArrayList<>();
        for (Servico s : servicos) {
            resposta.add(paraMapa(s));
        }

        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("quantidade", resposta.size());
        saida.put("servicos", resposta);
        if (resposta.isEmpty()) {
            saida.put("aviso", termo.isBlank()
                    ? "Nenhum servico cadastrado."
                    : "Nenhum servico encontrado para '" + termo + "'. Consulte a lista completa.");
        }
        return saida;
    }

    private Object tempoServico(Map<String, Object> args) {
        Servico servico = resolverServico(args);
        Map<String, Object> saida = new LinkedHashMap<>(paraMapa(servico));
        saida.put("orientacao", "Some as duracoes de todos os servicos desejados "
                + "e use o total em verificar_disponibilidade.");
        return saida;
    }

    private Object combo(Map<String, Object> args) {
        List<Long> ids = Argumentos.listaDeIds(args, "servico_ids");
        if (ids.isEmpty()) {
            throw new ConversaAgenteException("Informe ao menos um servico_id em 'servico_ids'.");
        }

        List<Servico> servicos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int duracaoTotal = 0;
        for (Long id : ids) {
            Servico servico = repository.findById(id)
                    .orElseThrow(() -> new ConversaAgenteException("Servico com id " + id + " nao existe."));
            servicos.add(servico);
            total = total.add(servico.getPreco());
            duracaoTotal += servico.getDuracaoMinutos();
        }

        List<Map<String, Object>> itens = new ArrayList<>();
        for (Servico s : servicos) {
            itens.add(paraMapa(s));
        }

        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("servicos", itens);
        saida.put("duracao_total_minutos", duracaoTotal);
        saida.put("preco_total", total);
        saida.put("orientacao", "Agende uma unica reserva com duracao_total_minutos="
                + duracaoTotal + " para todos os servicos em sequencia.");
        return saida;
    }

    private Servico resolverServico(Map<String, Object> args) {
        Long id = Argumentos.id(args, "servico_id");
        if (id != null) {
            return repository.findById(id)
                    .orElseThrow(() -> new ConversaAgenteException("Servico com id " + id + " nao existe."));
        }

        String nome = Argumentos.texto(args, "servico_nome");
        if (nome.isBlank()) {
            throw new ConversaAgenteException("Informe 'servico_id' ou 'servico_nome'.");
        }

        List<Servico> encontrados = repository.buscarPorTexto(nome);
        if (encontrados.isEmpty()) {
            throw new ConversaAgenteException("Nenhum servico encontrado para '" + nome + "'. Use listar_servicos.");
        }
        if (encontrados.size() > 1) {
            List<String> nomes = encontrados.stream().map(Servico::getNome).toList();
            throw new ConversaAgenteException("Mais de um servico combina com '" + nome + "': " + nomes
                    + ". Pergunte ao cliente qual ele prefere.");
        }
        return encontrados.get(0);
    }

    private Map<String, Object> paraMapa(Servico s) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", s.getId());
        mapa.put("nome", s.getNome());
        mapa.put("descricao", s.getDescricao());
        mapa.put("preco", s.getPreco());
        mapa.put("duracao_minutos", s.getDuracaoMinutos());
        mapa.put("combo", s.getCombo());
        return mapa;
    }
}
