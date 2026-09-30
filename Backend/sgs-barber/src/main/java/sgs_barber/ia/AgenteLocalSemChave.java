package sgs_barber.ia;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import sgs_barber.ia.ferramenta.FerramentasAgendamento;
import sgs_barber.ia.ferramenta.FerramentasBarbeiro;
import sgs_barber.ia.ferramenta.FerramentasServico;
import sgs_barber.ia.ferramenta.RegistroFerramentas;
import sgs_barber.ia.util.Datas;
import sgs_barber.ia.util.Texto;
import sgs_barber.model.TipoAgente;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agente de reserva baseado em regras, usado quando nao ha chave de IA
 * configurada ou quando o provedor esta fora do ar.
 *
 * Nao substitui o modelo: cobre o fluxo feliz (mostrar servicos, barbeiros e
 * os proximos horarios livres) para que a demonstracao da arquitetura
 * funcione numa maquina sem credencial. Ele consulta exatamente as mesmas
 * tools do modelo, o que tambem serve de teste de fumaça do catalogo.
 */
public final class AgenteLocalSemChave {

    private static final int DIAS_BUSCADOS = 5;

    /**
     * O agente local consulta as mesmas tools do modelo, mas nao decide marcar:
     * quem cria o agendamento e a tool {@code criar_agendamento}, chamada pelo
     * modelo. Dizer isso evita que o cliente acredite que o horario foi
     * reservado quando nao foi — e o {@code aviso} da resposta ja sinaliza que
     * a resposta veio por regras.
     */
    private static final String LIMITE_SEM_MODELO =
            "Para eu confirmar a reserva Configure IA_API_KEY no servidor; "
                    + "sem modelo eu apenas mostro a disponibilidade. "
                    + "Ate la, use o agendamento manual em \"Novo Agendamento\".";

    private AgenteLocalSemChave() {
    }

    /**
     * Monta a resposta por regras e devolve tambem as tools consultadas, para o
     * front exibir o mesmo rastro que exibiria quando quem responde e o modelo.
     */
    public static RespostaLocal responder(String mensagem, RegistroFerramentas registro, TipoAgente agente) {
        Coleta coleta = new Coleta();
        return new RespostaLocal(texto(mensagem, registro, agente, coleta), coleta);
    }

    private static String texto(String mensagem, RegistroFerramentas registro, TipoAgente agente, Coleta coleta) {
        // Preco vence agenda: "quanto custa cortar a barba?" e duvida de valor,
        // mesmo contendo o verbo "cortar". Sem esta ordem a pergunta cairia na
        // agenda e o usuario receberia horarios em vez do preco.
        if (Texto.contemAlgum(mensagem, "quanto custa", "preco", "custa", "valor",
                "duracao", "quanto tempo", "servico", "quanto sai")) {
            return responderServicos(mensagem, registro, coleta);
        }
        // A busca e por substring, entao a lista precisa trazer as formas
        // conjugadas que as pessoas realmente digitam. "horario" sozinho nao
        // cobre "horas", e "cortar" nao cobre "o Daniel corta amanha" - sem
        // isso a frase caia no menu de ajuda em vez de mostrar a agenda.
        if (Texto.contemAlgum(mensagem, "horario", "horas", "vaga", "livre", "disponib", "agendar", "marcar",
                "reservar", "cortar", "corta", "cabe", "quero um", "quero fazer", "corte de",
                "um horario", "dia da semana", "semana que vem", "vai cortar", "posso cortar")) {
            return responderAgenda(mensagem, registro, agente, coleta);
        }
        if (Texto.contemAlgum(mensagem, "barbeiro", "profissional", "especialista", "quem faz")) {
            return responderBarbeiros(registro, coleta);
        }
        if (Texto.contemAlgum(mensagem, "oi", "ola", "bom dia", "boa tarde", "boa noite", "e ai")) {
            return saudacao(registro, coleta);
        }
        return ajuda(registro, agente, coleta);
    }

    // ------------------------------------------------------------------

    private static String saudacao(RegistroFerramentas registro, Coleta coleta) {
        List<Map<String, Object>> servicos = servicos(registro, coleta);
        StringBuilder sb = new StringBuilder();
        sb.append("Ola! Sou o assistente do SGS-BARBER. Posso listar servicos, mostrar a equipe ")
                .append("e checar os horarios livres de cada barbeiro.\n\n");
        sb.append("**Servicos cadastrados:**\n");
        if (servicos.isEmpty()) {
            sb.append("- nenhum servico cadastrado ainda\n");
        }
        for (Map<String, Object> s : servicos) {
            sb.append("- ").append(s.get("nome"))
                    .append(" (").append(s.get("duracao_minutos")).append(" min, R$ ")
                    .append(s.get("preco")).append(")\n");
        }
        return sb.toString().trim();
    }

    private static String responderServicos(String mensagem, RegistroFerramentas registro, Coleta coleta) {
        String termo = "";
        for (String candidato : new String[]{"barba", "corte", "degrade", "terapia", "luz", "platinum"}) {
            if (Texto.contem(mensagem, candidato)) {
                termo = candidato;
                break;
            }
        }

        List<Map<String, Object>> servicos = servicos(registro, coleta);
        if (!termo.isEmpty()) {
            String alvo = Texto.normalizar(termo);
            servicos = servicos.stream()
                    .filter(s -> Texto.normalizar(String.valueOf(s.get("nome"))).contains(alvo))
                    .toList();
        }

        if (servicos.isEmpty()) {
            return "Nao encontrei servico para \"" + termo + "\". Os cadastrados sao: "
                    + nomes(servicos(registro, coleta)) + ".";
        }

        StringBuilder sb = new StringBuilder("Encontrei estes servicos:\n");
        for (Map<String, Object> s : servicos) {
            sb.append("- ").append(s.get("nome"))
                    .append(" — ").append(s.get("duracao_minutos")).append(" min, R$ ")
                    .append(s.get("preco"));
            if (s.get("descricao") != null && !String.valueOf(s.get("descricao")).isBlank()) {
                sb.append(" (").append(s.get("descricao")).append(")");
            }
            sb.append("\n");
        }
        sb.append("\nQuer que eu verifique a disponibilidade de algum deles?");
        return sb.toString().trim();
    }

    private static String responderBarbeiros(RegistroFerramentas registro, Coleta coleta) {
        List<Map<String, Object>> barbeiros = barbeiros(registro, coleta);
        if (barbeiros.isEmpty()) {
            return "Nenhum barbeiro ativo cadastrado no momento.";
        }

        StringBuilder sb = new StringBuilder("Nossa equipe:\n");
        for (Map<String, Object> b : barbeiros) {
            sb.append("- ").append(b.get("nome"))
                    .append(" — ").append(juntar(b.get("especialidades")))
                    .append(" (tel. ").append(b.get("telefone")).append(")\n");
        }
        sb.append("\nQuer marcar com algum deles?");
        return sb.toString().trim();
    }

    private static String responderAgenda(String mensagem, RegistroFerramentas registro, TipoAgente agente, Coleta coleta) {
        if (agente == TipoAgente.RECOMENDADOR) {
            return "Para marcar, o Agente Agendador assume a conversa. Diga o servico, o barbeiro e "
                    + "o dia desejado que eu verifico os horarios livres.";
        }

        List<Map<String, Object>> barbeiros = barbeiros(registro, coleta);
        if (barbeiros.isEmpty()) {
            return "Nenhum barbeiro ativo cadastrado, entao nao ha horario para consultar.";
        }

        Long escolhido = null;
        for (Map<String, Object> b : barbeiros) {
            if (Texto.contem(mensagem, String.valueOf(b.get("nome")))) {
                escolhido = Long.valueOf(String.valueOf(b.get("id")));
                break;
            }
        }

        if (escolhido == null) {
            return "Com qual barbeiro voce quer marcar? Temos: " + nomesBarbeiros(barbeiros)
                    + ". Me diga o nome que eu busco os horarios livres.";
        }

        final Long barbeiroEscolhido = escolhido;
        int duracao = duracaoPadrao(registro, mensagem, coleta);
        String nomeEscolhido = barbeiros.stream()
                .filter(b -> String.valueOf(b.get("id")).equals(String.valueOf(barbeiroEscolhido)))
                .map(b -> String.valueOf(b.get("nome")))
                .findFirst()
                .orElse("o barbeiro");

        // "amanha", "sexta", "15/10" precisam ser respeitados; sem expressao de
        // data no texto, comeca por hoje.
        LocalDate inicio = Datas.resolverData(mensagem);
        boolean dataExplicita = inicio != null;
        if (!dataExplicita) {
            inicio = LocalDate.now();
        }
        // Se a pessoa disse o dia ("amanha"), despejar 5 dias nela le como
        // "esqueci de filtrar". So abre a janela toda quando nao ha data.
        int diasAConsultar = dataExplicita ? 1 : DIAS_BUSCADOS;

        // "as 14h" e "de tarde" sao pedidos, nao ruido: sem isto o usuario
        // recebe a agenda inteira, inclusive a manha que ele nao pediu.
        LocalTime horaAlvo = Datas.resolverHora(mensagem);
        LocalTime[] janela = Datas.janelaDoDia(mensagem);
        if (horaAlvo != null && janela == null) {
            janela = Datas.janelaDoDia(periodoDaHora(horaAlvo));
        }

        Map<String, Object> resultado = executar(registro, agente, FerramentasAgendamento.VERIFICAR_DISPONIBILIDADE,
                Map.of(
                        "barbeiro_id", escolhido,
                        "data", inicio.toString(),
                        "duracao_total_minutos", duracao,
                        "dias_a_consultar", diasAConsultar), coleta);

        return montarAgenda(nomeEscolhido, duracao, horaAlvo, janela, resultado, diasAConsultar);
    }

    /** Periodo do dia a que uma hora isolada pertence, para filtrar a agenda. */
    private static String periodoDaHora(LocalTime hora) {
        if (hora.getHour() < 12) {
            return "manha";
        }
        return hora.getHour() < 18 ? "tarde" : "noite";
    }

    private static String montarAgenda(String nomeBarbeiro, int duracao, LocalTime horaAlvo,
                                      LocalTime[] janela, Map<String, Object> resultado, int diasAConsultar) {
        Object dias = resultado.get("dias");
        if (!(dias instanceof List<?> listaDias)) {
            return "Nao consegui consultar a agenda do " + nomeBarbeiro + ".";
        }

        String periodo = janela == null ? null : Texto.normalizar(nomeDaJanela(janela));

        // Hora exata pedida: responde o que foi pedido, em vez de despejar a
        // agenda inteira. "as 14h" tem que gerar "14:00 esta livre" ou uma
        // recusa util com as alternativas, nunca uma lista sem relacao.
        if (horaAlvo != null) {
            return montarHoraExata(nomeBarbeiro, duracao, horaAlvo, resultado, diasAConsultar);
        }

        StringBuilder sb = new StringBuilder("Horarios livres do ").append(nomeBarbeiro)
                .append(" para ").append(duracao).append(" min");
        sb.append(periodo == null ? ":\n" : " " + preposicaoDoPeriodo(periodo) + ":\n");

        boolean achou = false;
        for (Object item : listaDias) {
            if (!(item instanceof Map<?, ?> dia)) {
                continue;
            }
            List<String> horas = slotsDoDia(dia);
            horas = filtrarPeriodo(horas, janela);
            if (horas.isEmpty()) {
                continue;
            }
            achou = true;
            sb.append("- ").append(dia.get("dia_semana"))
                    .append(" (").append(dataBr(String.valueOf(dia.get("data")))).append("): ");
            sb.append(String.join(", ", horas)).append("\n");
        }

        if (!achou) {
            return semHorarios(nomeBarbeiro, duracao, diasAConsultar, periodo);
        }
        sb.append("\n\nEscolha um horario acima. ").append(LIMITE_SEM_MODELO);
        return sb.toString().trim();
    }

    /** Resposta para "as 14h": confirma se existe, ou recusa com alternativas. */
    private static String montarHoraExata(String nomeBarbeiro, int duracao, LocalTime horaAlvo,
                                         Map<String, Object> resultado, int diasAConsultar) {
        Object dias = resultado.get("dias");
        List<?> listaDias = dias instanceof List<?> lista ? lista : List.of();

        for (Object item : listaDias) {
            if (!(item instanceof Map<?, ?> dia)) {
                continue;
            }
            List<String> horas = slotsDoDia(dia);
            if (!horas.contains(horaTexto(horaAlvo))) {
                continue;
            }
            String quando = dia.get("dia_semana") + " (" + dataBr(String.valueOf(dia.get("data"))) + ")";
            return horaTexto(horaAlvo) + " esta livre com " + nomeBarbeiro + " " + quando
                    + ", para " + duracao + " min.\n\n" + LIMITE_SEM_MODELO;
        }

        // Nao ha essa hora. Oferece o mais proximo do mesmo periodo, que e o
        // que a pessoa realmente quer, em vez de "nao tem".
        LocalTime[] janela = Datas.janelaDoDia(periodoDaHora(horaAlvo));
        StringBuilder alternativas = new StringBuilder();
        for (Object item : listaDias) {
            if (!(item instanceof Map<?, ?> dia)) {
                continue;
            }
            List<String> horas = filtrarPeriodo(slotsDoDia(dia), janela);
            if (horas.isEmpty()) {
                continue;
            }
            alternativas.append("- ").append(dia.get("dia_semana"))
                    .append(" (").append(dataBr(String.valueOf(dia.get("data")))).append("): ")
                    .append(String.join(", ", proximas(horas, horaAlvo, 6))).append("\n");
        }

        String quando = diasAConsultar == 1
                ? "nesse dia"
                : "nos proximos " + diasAConsultar + " dias";
        if (alternativas.isEmpty()) {
            return "O " + nomeBarbeiro + " nao tem horario livre " + quando + " para " + duracao + " min.\n\n"
                    + LIMITE_SEM_MODELO;
        }
        return horaTexto(horaAlvo) + " nao esta livre com " + nomeBarbeiro + " " + quando
                + ". Estas sao as opcoes mais proximas:\n" + alternativas + "\n" + LIMITE_SEM_MODELO;
    }

    private static List<String> slotsDoDia(Map<?, ?> dia) {
        Object slots = dia.get("horarios_livres");
        if (!(slots instanceof List<?> lista)) {
            return new ArrayList<>();
        }
        List<String> horas = new ArrayList<>();
        for (Object slot : lista) {
            horas.add(String.valueOf(slot));
        }
        return horas;
    }

    /** Mantem apenas os horarios dentro da janela pedida ("manha", "tarde"...). */
    private static List<String> filtrarPeriodo(List<String> horas, LocalTime[] janela) {
        if (janela == null) {
            return horas;
        }
        List<String> dentro = new ArrayList<>();
        for (String hora : horas) {
            try {
                if (Datas.dentroDaJanela(LocalTime.parse(hora), janela[0], janela[1])) {
                    dentro.add(hora);
                }
            } catch (RuntimeException e) {
                dentro.add(hora);
            }
        }
        return dentro;
    }

    /** "09:00, 10:15..." a partir da hora pedida, para sugerir o mais proximo. */
    private static List<String> proximas(List<String> horas, LocalTime alvo, int limite) {
        List<String> ordenadas = new ArrayList<>(horas);
        ordenadas.sort((a, b) -> Long.compare(distancia(a, alvo), distancia(b, alvo)));
        return ordenadas.subList(0, Math.min(limite, ordenadas.size()));
    }

    /** "HH:mm" da hora pedida. Datas.formatar exige a data e devolveria vazio. */
    private static String horaTexto(LocalTime hora) {
        return hora.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private static long distancia(String hora, LocalTime alvo) {
        try {
            return Math.abs(LocalTime.parse(hora).toSecondOfDay() - alvo.toSecondOfDay());
        } catch (RuntimeException e) {
            return Long.MAX_VALUE;
        }
    }

    /** "a tarde", "de manha", "a noite": a frase precisa soar natural. */
    private static String preposicaoDoPeriodo(String periodo) {
        return switch (periodo) {
            case "manha" -> "de manha";
            case "noite" -> "a noite";
            default -> "a " + periodo;
        };
    }

    private static String nomeDaJanela(LocalTime[] janela) {
        if (janela[0].getHour() < 12) {
            return "manha";
        }
        return janela[0].getHour() < 18 ? "tarde" : "noite";
    }

    private static String semHorarios(String nomeBarbeiro, int duracao, int diasAConsultar, String periodo) {
        String quando = diasAConsultar == 1
                ? "nesse dia"
                : "nos proximos " + diasAConsultar + " dias";
        String onde = periodo == null ? "" : " " + preposicaoDoPeriodo(periodo);
        return "O " + nomeBarbeiro + " nao tem horario livre" + onde + " " + quando
                + " para " + duracao + " min.";
    }

    /** A tool devolve a data em ISO; o cliente le melhor em dd/MM/yyyy. */
    private static String dataBr(String iso) {
        try {
            return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (RuntimeException e) {
            return iso;
        }
    }

    private static String ajuda(RegistroFerramentas registro, TipoAgente agente, Coleta coleta) {
        StringBuilder sb = new StringBuilder("Posso ajudar com:\n");
        sb.append("- **Servicos**: precos e duracoes (ex.: \"quanto custa a barba?\")\n");
        sb.append("- **Equipe**: barbeiros e especialidades (ex.: \"quem faz barba?\")\n");
        sb.append("- **Agenda**: horarios livres (ex.: \"ha horario com o Carlos amanha?\")\n");
        if (agente == TipoAgente.RECOMENDADOR) {
            sb.append("- **Estilo**: qual corte combina com o que voce descreveu\n");
        }
        return sb.toString().trim();
    }

    // ------------------------------------------------------------------

    private static int duracaoPadrao(RegistroFerramentas registro, String mensagem, Coleta coleta) {
        for (String palavra : new String[]{"barba", "corte", "terapia", "luz", "platinum", "degrade"}) {
            if (Texto.contem(mensagem, palavra)) {
                Map<String, Object> servico = executar(registro, TipoAgente.AGENDADOR, FerramentasServico.OBTER_TEMPO_SERVICO,
                        Map.of("servico_nome", palavra), coleta);
                Object duracao = servico.get("duracao_minutos");
                if (duracao instanceof Number numero) {
                    return numero.intValue();
                }
            }
        }
        // 45 min:_media de corte + barba, razoavel para uma primeira resposta.
        return 45;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> executar(RegistroFerramentas registro, TipoAgente agente, String ferramenta,
                                                Map<String, Object> argumentos, Coleta coleta) {
        String json = registro.executar(ferramenta, argumentos, agente);
        try {
            Map<String, Object> lido = new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {
            });
            // Registra o que foi realmente consultado para o front-mostrar o mesmo
            // rastro de tools que aparece quando quem responde e o modelo.
            coleta.registrar(ferramenta, argumentos, lido);
            return lido;
        } catch (Exception e) {
            coleta.registrar(ferramenta, argumentos, Map.of("erro", "resposta ilegivel"));
            return Map.of("erro", "nao foi possivel ler a resposta da tool");
        }
    }

    /**
     * Tools consultadas por uma resposta do agente local. Existe para que o
     * trace devolvido ao front nao dependa de haver modelo: sem chave, o
     * cliente ainda precisa ver de onde vieram os dados.
     */
    /** Texto gerado + rastro de tools do agente local. */
    public record RespostaLocal(String texto, Coleta coleta) {
    }

    public static final class Coleta {

        private final List<Map<String, Object>> ferramentas = new ArrayList<>();

        void registrar(String nome, Map<String, Object> argumentos, Map<String, Object> resultado) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("nome", nome);
            item.put("argumentos", argumentos == null ? Map.of() : argumentos);
            item.put("resultado", String.valueOf(resultado));
            ferramentas.add(item);
        }

        public List<Map<String, Object>> ferramentas() {
            return ferramentas;
        }
    }


    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> servicos(RegistroFerramentas registro, Coleta coleta) {
        Map<String, Object> resposta = executar(registro, TipoAgente.RECOMENDADOR, FerramentasServico.LISTAR_SERVICOS, Map.of(), coleta);
        Object lista = resposta.get("servicos");
        return lista instanceof List<?> ? (List<Map<String, Object>>) lista : List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> barbeiros(RegistroFerramentas registro, Coleta coleta) {
        Map<String, Object> resposta = executar(registro, TipoAgente.RECOMENDADOR, FerramentasBarbeiro.CONSULTAR_BARBEIROS, Map.of(), coleta);
        Object lista = resposta.get("barbeiros");
        return lista instanceof List<?> ? (List<Map<String, Object>>) lista : List.of();
    }

    private static String nomes(List<Map<String, Object>> itens) {
        List<String> valores = new ArrayList<>();
        for (Map<String, Object> item : itens) {
            valores.add(String.valueOf(item.get("nome")));
        }
        return valores.isEmpty() ? "nenhum" : String.join(", ", valores);
    }

    private static String nomesBarbeiros(List<Map<String, Object>> itens) {
        return nomes(itens);
    }

    private static String juntar(Object lista) {
        if (lista instanceof List<?> valores && !valores.isEmpty()) {
            return String.join(", ", valores.stream().map(String::valueOf).toList());
        }
        return "especialidades nao informadas";
    }
}
