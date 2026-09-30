package sgs_barber.ia.util;

import java.text.Normalizer;

/** Normalização de texto para busca: ignora acento e caixa. */
public final class Texto {

    private Texto() {
    }

    public static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase().trim();
    }

    public static boolean contem(String texto, String trecho) {
        return normalizar(texto).contains(normalizar(trecho));
    }

    public static boolean contemAlgum(String texto, String... trechos) {
        for (String trecho : trechos) {
            if (contem(texto, trecho)) {
                return true;
            }
        }
        return false;
    }
}
