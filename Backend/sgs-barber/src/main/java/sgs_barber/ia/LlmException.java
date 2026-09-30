package sgs_barber.ia;

/** Erro de integração com o provedor de IA (rede, credencial, JSON inválido). */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
