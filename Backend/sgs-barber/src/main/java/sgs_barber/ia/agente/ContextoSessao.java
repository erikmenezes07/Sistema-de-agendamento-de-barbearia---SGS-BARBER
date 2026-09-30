package sgs_barber.ia.agente;

import org.springframework.stereotype.Component;
import sgs_barber.ia.util.Datas;
import sgs_barber.model.Barbeiro;
import sgs_barber.repository.BarbeiroRepository;
import sgs_barber.repository.ServicoRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

/**
 * Monta o bloco de contexto que entra no prompt de sistema.
 *
 * Passar a data real e o dia da semana por extenso Ǹ o que impede o erro mais
 * comum desse agentes: o modelo nao sabe que "sexta" de hoje ja passou.
 *
 * O catálogo (serviços e equipe) também entra aqui de propósito. Sem ele, o
 * modelo precisa chamar {@code listar_servicos} e {@code consultar_barbeiros}
 * só para descobrir preço e nome, gastando uma ida ao provedor a cada
 * mensagem. O bloco é pequeno e muda raramente, então mandá-lo no prompt é
 * bem mais barato que deixar o modelo descobrir por tool. O cache com TTL
 * evita ir ao banco a cada mensagem, mas expira rápido para que um serviço
 * cadastrado no painel apareça no chat sem reiniciar a aplicação.
 */
@Component
public class ContextoSessao {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Duration VALIDADE_CATALOGO = Duration.ofSeconds(60);

    private final ServicoRepository servicoRepository;
    private final BarbeiroRepository barbeiroRepository;

    private volatile String catalogoCacheado = "";
    private volatile Instant catalogoGeradoEm = Instant.EPOCH;

    public ContextoSessao(ServicoRepository servicoRepository, BarbeiroRepository barbeiroRepository) {
        this.servicoRepository = servicoRepository;
        this.barbeiroRepository = barbeiroRepository;
    }

    public String montar(String clienteNome, Long clienteId) {
        LocalDate hoje = LocalDate.now();
        StringBuilder sb = new StringBuilder();

        sb.append("- Data de hoje: ").append(Datas.nomeDoDia(hoje))
                .append(" (").append(hoje).append("). Agora sao ")
                .append(LocalTime.now().format(FORMATO_HORA)).append(".\n");
        sb.append("- Ao falar de dia da semana, converta sempre para data: \"sexta\" = ")
                .append(Datas.resolverData("sexta")).append(".\n");
        sb.append("- Barbeiros atendem por expediente configurado; a pausa de almoco bloqueia o horario.\n");

        if (clienteNome != null && !clienteNome.isBlank()) {
            sb.append("- Cliente desta sessao: ").append(clienteNome);
            if (clienteId != null) {
                sb.append(" (cliente_id=").append(clienteId).append(")");
            }
            sb.append(". Use o cliente_id ao criar agendamento.\n");
        } else {
            sb.append("- Cliente desta sessao: nao identificado. Use buscar_cliente antes de agendar.\n");
        }

        sb.append(catalogo());
        return sb.toString();
    }

    /**
     * Monta o catálogo uma vez por TTL. Falha de banco aqui não pode derrubar
     * o chat: nesse caso devolve string vazia e o agente continua usando as
     * tools normalmente.
     */
    private String catalogo() {
        String cache = catalogoCacheado;
        if (!cache.isEmpty() && Instant.now().isBefore(catalogoGeradoEm.plus(VALIDADE_CATALOGO))) {
            return cache;
        }
        try {
            cache = montarCatalogo();
            catalogoCacheado = cache;
            catalogoGeradoEm = Instant.now();
        } catch (Exception e) {
            return "";
        }
        return cache;
    }

    private String montarCatalogo() {
        StringBuilder sb = new StringBuilder();
        List<Barbeiro> equipe = barbeiroRepository.findByAtivoTrueOrderByNomeAsc();
        sb.append("- Equipe ativa (id | nome | especialidades):");
        if (equipe.isEmpty()) {
            sb.append(" nenhum barbeiro ativo cadastrado.");
        } else {
            for (Barbeiro b : equipe) {
                sb.append("\n  * ").append(b.getId()).append(" | ").append(b.getNome())
                        .append(" | ").append(b.getEspecialidades() == null ? "sem especialidade" : b.getEspecialidades());
            }
        }

        sb.append("\n- Servicos cadastrados (id | nome | duracao | preco):");
        List<?> servicos = servicoRepository.findAll();
        if (servicos.isEmpty()) {
            sb.append(" nenhum servico cadastrado.");
        } else {
            for (Object item : servicos) {
                sb.append("\n  * ").append(campo(item, "id")).append(" | ").append(campo(item, "nome"))
                        .append(" | ").append(campo(item, "duracaoMinutos")).append(" min | R$ ")
                        .append(campo(item, "preco"));
            }
        }
        sb.append("\n- Use estas listas para responder preco e equipe sem chamar ferramenta. "
                + "Chame ferramenta apenas para agenda, cliente e acao que mude dados.\n");
        return sb.toString();
    }

    /**
     * Acesso por nome para não acoplar este classe às entidades: assim uma
     * mudança no modelo de dados não quebra a montagem do prompt.
     */
    private static Object campo(Object entidade, String atributo) {
        try {
            var metodo = entidade.getClass().getMethod("get"
                    + atributo.substring(0, 1).toUpperCase() + atributo.substring(1));
            return metodo.invoke(entidade);
        } catch (ReflectiveOperationException e) {
            return "?";
        }
    }
}
