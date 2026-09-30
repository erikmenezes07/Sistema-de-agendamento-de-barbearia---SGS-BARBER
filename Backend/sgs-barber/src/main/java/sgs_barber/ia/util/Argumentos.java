package sgs_barber.ia.util;

import sgs_barber.ia.ConversaAgenteException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Leitura tipada e tolerante dos argumentos que o modelo envia.
 *
 * Modelos pequeno-medium as vezes mandam "45" como texto ou esquecem um campo
 * opcional. Em vez de estourar erro de desserializacao, a tool pede de volta
 * com uma mensagem clara, e o modelo corrige na proxima chamada.
 */
public final class Argumentos {

    private Argumentos() {
    }

    public static String texto(Map<String, Object> args, String chave) {
        Object valor = args.get(chave);
        if (valor == null) {
            return "";
        }
        String texto = String.valueOf(valor).trim();
        return texto.equalsIgnoreCase("null") ? "" : texto;
    }

    public static String textoObrigatorio(Map<String, Object> args, String chave) {
        String valor = texto(args, chave);
        if (valor.isEmpty()) {
            throw new ConversaAgenteException("O argumento '" + chave + "' e obrigatorio.");
        }
        return valor;
    }

    public static Long id(Map<String, Object> args, String chave) {
        String valor = texto(args, chave);
        if (valor.isEmpty()) {
            return null;
        }
        try {
            // aceita "1" e 1, e tambem o "barbeiro_id=1" que o modelo as vezes manda
            String digitos = valor.replaceAll("\\D+", "");
            return digitos.isEmpty() ? null : Long.parseLong(digitos);
        } catch (NumberFormatException e) {
            throw new ConversaAgenteException("O argumento '" + chave + "' deveria ser um id numerico, veio '" + valor + "'.");
        }
    }

    public static Long idObrigatorio(Map<String, Object> args, String chave) {
        Long valor = id(args, chave);
        if (valor == null) {
            throw new ConversaAgenteException("O argumento '" + chave + "' e obrigatorio.");
        }
        return valor;
    }

    public static Integer inteiro(Map<String, Object> args, String chave, Integer padrao) {
        Object valor = args.get(chave);
        if (valor == null) {
            return padrao;
        }
        if (valor instanceof Number numero) {
            return numero.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(valor).replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            throw new ConversaAgenteException("O argumento '" + chave + "' deveria ser um numero inteiro.");
        }
    }

    public static Boolean booleano(Map<String, Object> args, String chave, Boolean padrao) {
        Object valor = args.get(chave);
        if (valor == null) {
            return padrao;
        }
        if (valor instanceof Boolean booleano) {
            return booleano;
        }
        String texto = String.valueOf(valor).trim().toLowerCase();
        if (texto.equals("true") || texto.equals("sim") || texto.equals("1")) {
            return Boolean.TRUE;
        }
        if (texto.equals("false") || texto.equals("nao") || texto.equals("não") || texto.equals("0")) {
            return Boolean.FALSE;
        }
        return padrao;
    }

    public static List<Long> listaDeIds(Map<String, Object> args, String chave) {
        Object valor = args.get(chave);
        List<Long> ids = new java.util.ArrayList<>();
        if (valor == null) {
            return ids;
        }
        if (valor instanceof List<?> lista) {
            for (Object item : lista) {
                String digitos = String.valueOf(item).replaceAll("\\D+", "");
                if (!digitos.isEmpty()) {
                    ids.add(Long.parseLong(digitos));
                }
            }
        } else {
            Long unico = id(args, chave);
            if (unico != null) {
                ids.add(unico);
            }
        }
        return ids;
    }

    /** Resolve o campo de data, aceitando ISO ou texto natural ("sexta", "amanha"). */
    public static LocalDate data(Map<String, Object> args, String chave) {
        String valor = texto(args, chave);
        if (valor.isEmpty()) {
            return null;
        }
        return Datas.resolverData(valor);
    }

    public static LocalTime hora(Map<String, Object> args, String chave) {
        String valor = texto(args, chave);
        if (valor.isEmpty()) {
            return null;
        }
        return Datas.resolverHora(valor);
    }
}
