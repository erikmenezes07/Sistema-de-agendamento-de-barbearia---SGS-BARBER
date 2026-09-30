package sgs_barber.ia.util;

import sgs_barber.ia.ConversaAgenteException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converte expressoes naturais de data e hora em tipos nativos de Java.
 *
 * Existe porque o modelo nem sempre normaliza a data: ele acaba passando
 * "sexta", "amanha de tarde" ou "05/10". Resolver em um unico lugar deixa as
 * tools simples e evita erro de data por parsing repetido dentro do prompt.
 */
public final class Datas {

    private Datas() {
    }

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATO_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR);
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static final Pattern ISO = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
    private static final Pattern BR = Pattern.compile("(\\d{1,2})[/\\-.](\\d{1,2})[/\\-.](\\d{2,4})");
    private static final Pattern EM_N_DIAS = Pattern.compile("em\\s+(\\d{1,2})\\s+dias?");
    private static final Pattern DIA_SEMANA = Pattern.compile(
            "(segunda|terca|quarta|quinta|sexta|sabado|domingo)(?:[- ]?feira)?");
    private static final Pattern HORA_COM_MINUTO = Pattern.compile("(\\d{1,2})\\s*[:hH]\\s*(\\d{2})");
    private static final Pattern HORA_SIMPLES = Pattern.compile("(\\d{1,2})\\s*(?:h\\b|horas?\\b|hrs?\\b)");

    private static final Pattern JANELA_MANHA = Pattern.compile("\\b(manha|manhas|madrugada|cedo|antes do meio-dia)\\b");
    private static final Pattern JANELA_TARDE = Pattern.compile("\\b(tarde|tardes|meio-dia|pos-edime|apos o almoco)\\b");
    private static final Pattern JANELA_NOITE = Pattern.compile("\\b(noite|noites|anoitecer|fim da tarde)\\b");

    private static final String[] NOMES_DIAS = {
            "segunda-feira", "terça-feira", "quarta-feira",
            "quinta-feira", "sexta-feira", "sábado", "domingo"
    };

    /**
     * Resolve a data informada, ou devolve null quando nao ha expressao de data
     * reconhecivel. O chamador decide o que fazer com isso.
     */
    public static LocalDate resolverData(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String normalizado = Texto.normalizar(texto);

        Matcher iso = ISO.matcher(texto);
        if (iso.find()) {
            return montarData(Integer.parseInt(iso.group(1)),
                    Integer.parseInt(iso.group(2)),
                    Integer.parseInt(iso.group(3)));
        }

        Matcher br = BR.matcher(texto);
        if (br.find()) {
            int ano = Integer.parseInt(br.group(3));
            if (ano < 100) {
                ano += 2000;
            }
            return montarData(ano, Integer.parseInt(br.group(2)), Integer.parseInt(br.group(1)));
        }

        if (normalizado.contains("depois de amanha")) {
            return LocalDate.now().plusDays(2);
        }
        if (normalizado.contains("amanha")) {
            return LocalDate.now().plusDays(1);
        }
        if (normalizado.contains("hoje")) {
            return LocalDate.now();
        }
        if (normalizado.contains("ontem")) {
            return LocalDate.now().minusDays(1);
        }

        Matcher emDias = EM_N_DIAS.matcher(normalizado);
        if (emDias.find()) {
            return LocalDate.now().plusDays(Integer.parseInt(emDias.group(1)));
        }

        Matcher dia = DIA_SEMANA.matcher(normalizado);
        if (dia.find()) {
            return proximaData(diaDaSemana(dia.group(1)), normalizado);
        }

        return null;
    }

    /**
     * Traduz o nome do dia em portugues para {@link DayOfWeek}. Nao pode usar
     * {@code DayOfWeek.valueOf(...)} porque as constantes do enum sao em ingles
     * e "TERCA"/"SEXTA" lancariam IllegalArgumentException.
     */
    private static DayOfWeek diaDaSemana(String nome) {
        return switch (nome) {
            case "segunda" -> DayOfWeek.MONDAY;
            case "terca" -> DayOfWeek.TUESDAY;
            case "quarta" -> DayOfWeek.WEDNESDAY;
            case "quinta" -> DayOfWeek.THURSDAY;
            case "sexta" -> DayOfWeek.FRIDAY;
            case "sabado" -> DayOfWeek.SATURDAY;
            case "domingo" -> DayOfWeek.SUNDAY;
            default -> throw new ConversaAgenteException("Dia da semana nao reconhecido: " + nome);
        };
    }

    /** Resolve a data ou falha com mensagem que o modelo consegue corrigir. */
    public static LocalDate exigirData(String texto, String campo) {
        LocalDate data = resolverData(texto);
        if (data == null) {
            throw new ConversaAgenteException(
                    "Nao consegui entender a data informada em '" + campo + "' (" + texto + "). "
                            + "Use o formato AAAA-MM-DD, ou um termo como 'hoje', 'amanha' ou 'sexta-feira'.");
        }
        return data;
    }

    private static LocalDate montarData(int ano, int mes, int dia) {
        try {
            return LocalDate.of(ano, mes, dia);
        } catch (Exception e) {
            throw new ConversaAgenteException("Data invalida: " + dia + "/" + mes + "/" + ano);
        }
    }

    private static LocalDate proximaData(DayOfWeek alvo, String contexto) {
        LocalDate hoje = LocalDate.now();
        int delta = alvo.getValue() - hoje.getDayOfWeek().getValue();

        if (delta < 0) {
            // "nesta semana" mantem o dia da semana que ja passou (vai ser
            // rejeitado como data passada); caso contrario, vai para a proxima.
            boolean destaSemana = contexto.contains("esta ") || contexto.contains("esse ")
                    || contexto.contains("desta semana") || contexto.contains("nesta semana");
            if (!destaSemana) {
                delta += 7;
            }
        }
        return hoje.plusDays(delta);
    }

    public static String nomeDoDia(LocalDate data) {
        return NOMES_DIAS[data.getDayOfWeek().getValue() - 1];
    }

    /**
     * Resolve hora em "14h", "14:30", "14h30", "14 horas". Devolve null se nao
     * houver hora reconhecivel.
     */
    public static LocalTime resolverHora(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpo = Texto.normalizar(texto).replaceAll("\\s+", " ");

        Matcher comMinuto = HORA_COM_MINUTO.matcher(limpo);
        if (comMinuto.find()) {
            return montarHora(comMinuto.group(1), comMinuto.group(2));
        }

        Matcher simples = HORA_SIMPLES.matcher(limpo);
        if (simples.find()) {
            return montarHora(simples.group(1), "00");
        }

        return null;
    }

    private static LocalTime montarHora(String hora, String minuto) {
        int h = Integer.parseInt(hora);
        int m = Integer.parseInt(minuto);
        if (h < 0 || h > 23 || m < 0 || m > 59) {
            return null;
        }
        return LocalTime.of(h, m);
    }

    /** Janelas usadas por "manha", "tarde" e "noite". Null se nao houver periodo. */
    public static LocalTime[] janelaDoDia(String periodo) {
        String p = Texto.normalizar(periodo);
        // Limite de palavra e obrigatorio: em portugues "amanha" contem a
        // sequencia "manha", e um contains simples classificaria "amanha de
        // tarde" como manha.
        if (JANELA_MANHA.matcher(p).find()) {
            return new LocalTime[]{LocalTime.of(5, 0), LocalTime.of(12, 0)};
        }
        if (JANELA_TARDE.matcher(p).find()) {
            return new LocalTime[]{LocalTime.of(12, 0), LocalTime.of(18, 0)};
        }
        if (JANELA_NOITE.matcher(p).find()) {
            return new LocalTime[]{LocalTime.of(18, 0), LocalTime.of(23, 59)};
        }
        return null;
    }

    public static boolean dentroDaJanela(LocalTime hora, LocalTime inicio, LocalTime fim) {
        if (hora == null) {
            return true;
        }
        return !hora.isBefore(inicio) && !hora.isAfter(fim);
    }

    public static String formatar(LocalDate data, LocalTime hora) {
        if (data == null) {
            return "";
        }
        String base = nomeDoDia(data) + " (" + data.format(FORMATO_BR) + ")";
        return hora == null ? base : base + " as " + hora.format(FORMATO_HORA);
    }
}
