package sgs_barber.ia;

import java.util.List;
import java.util.Map;

/**
 * Contrato mínimo de um modelo com function calling. Existe para que o
 * orquestrador não dependa de nenhum fornecedor específico: trocar de
 * OpenAI para Ollama, por exemplo, é só trocar a implementação.
 */
public interface LlmClient {

    /**
     * @param mensagens mensagens no formato de chat (system/user/assistant/tool)
     * @param ferramentas schemas no formato OpenAI ({type:function, function:{...}})
     * @return resposta do modelo, com texto e/ou chamadas de ferramenta
     */
    RespostaLlm completar(List<Map<String, Object>> mensagens, List<Map<String, Object>> ferramentas);

    /** Identifica o provedor em log/erros. */
    String nomeProvedor();

    /**
     * Chamada de função solicitada pelo modelo.
     *
     * @param id identificador da chamada, usado no {@code tool_call_id} do resultado
     * @param nome nome da ferramenta
     * @param argumentos argumentos já desserializados
     * @param assinaturaThought assinatura que o Gemini devolve junto da chamada
     *                        ({@code extra_content.google.thought_signature}).
     *                        Precisa ser reenviada na mensagem do assistente no
     *                        turno seguinte, senão o Gemini responde 400
     *                        "Function call is missing a thought_signature".
     *                        Provedores compatíveis com OpenAI que não usam esse
     *                        conceito devolvem {@code null}.
     */
    record ChamadaFerramenta(String id, String nome, Map<String, Object> argumentos, String assinaturaThought) {

        public ChamadaFerramenta {
            argumentos = argumentos == null ? Map.of() : argumentos;
        }

        public ChamadaFerramenta(String id, String nome, Map<String, Object> argumentos) {
            this(id, nome, argumentos, null);
        }
    }

    /**
     * @param conteudo texto final do modelo (pode ser null quando o modelo
     *                 só pediu ferramentas)
     * @param chamadas ferramentas que o modelo quer executar
     * @param querEncerrar false se o modelo pediu execução de ferramenta e
     *                      ainda falta o resultado
     */
    record RespostaLlm(String conteudo, List<ChamadaFerramenta> chamadas, boolean querEncerrar) {

        public boolean somenteTexto() {
            return chamadas == null || chamadas.isEmpty();
        }
    }
}
