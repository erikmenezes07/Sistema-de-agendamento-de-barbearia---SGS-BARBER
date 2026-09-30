package sgs_barber.ia;

/**
 * Erro de negócio esperado durante a execução de uma tool. A mensagem é
 * devolvida ao modelo como feedback (em vez de estourar 500), para que ele
 * corrija a chamada ou explique o problema ao cliente.
 */
public class ConversaAgenteException extends RuntimeException {

    public ConversaAgenteException(String message) {
        super(message);
    }
}
