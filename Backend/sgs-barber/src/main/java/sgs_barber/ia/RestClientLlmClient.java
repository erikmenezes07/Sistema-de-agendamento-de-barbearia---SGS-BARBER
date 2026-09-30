package sgs_barber.ia;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import sgs_barber.ia.config.IaProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para modelos compatíveis com a API /chat/completions da OpenAI.
 *
 * O payload é montado com Map em vez de DTOs de propósito: assim o mesmo
 * código serve a provedores que acrescentam ou removem campos sem quebrar.
 */
public class RestClientLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(RestClientLlmClient.class);

    private final IaProperties props;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public RestClientLlmClient(IaProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(props.getTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(props.getTimeoutMs()));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String nomeProvedor() {
        return props.getBaseUrl();
    }

    @Override
    public RespostaLlm completar(List<Map<String, Object>> mensagens, List<Map<String, Object>> ferramentas) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("model", props.getModelo());
        corpo.put("messages", mensagens);
        corpo.put("temperature", props.getTemperatura());
        if (props.getMaxTokens() > 0) {
            corpo.put("max_tokens", props.getMaxTokens());
        }
        if (ferramentas != null && !ferramentas.isEmpty()) {
            corpo.put("tools", ferramentas);
            corpo.put("tool_choice", "auto");
        }

        // O plano gratuito cobra por token de entrada, e o payload cresce
        // rápido: prompt + schemas de todas as tools + historico. Medir ajuda a
        // saber onde a cota esta indo.
        try {
            String json = objectMapper.writeValueAsString(corpo);
            String prompt = mensagens.isEmpty() ? "" : String.valueOf(mensagens.get(0).get("content"));
            log.debug("Payload para o provedor: {} caracteres, {} ferramenta(s), {} mensagem(ns), "
                            + "prompt {} caracteres, catalogo {}",
                    json.length(),
                    ferramentas == null ? 0 : ferramentas.size(),
                    mensagens.size(),
                    prompt.length(),
                    prompt.contains("Servicos cadastrados") ? "sim" : "NAO");
        } catch (Exception e) {
            log.debug("Nao foi possivel medir o payload: {}", e.getMessage());
        }


        // Free tier derruba chamada grande com 429/503 ("high demand") mesmo
        // com cota de minuto livre. Sem retentativa o usuario ve o assistente
        // cair no fallback de regras e conclui que a IA esta quebrada. Duas
        // retentativas com espera crescente resolvem a maioria dos casos.
        int tentativas = Math.max(1, props.getMaxTentativas());
        LlmException ultimaFalha = null;
        for (int tentativa = 1; tentativa <= tentativas; tentativa++) {
            try {
                String jsonResposta = restClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(corpo)
                        .retrieve()
                        .body(String.class);
                return interpretar(jsonResposta);
            } catch (Exception e) {
                ultimaFalha = new LlmException("Falha ao chamar o modelo de linguagem: " + e.getMessage(), e);
                if (tentativa == tentativas || !transitorio(e)) {
                    throw ultimaFalha;
                }
                long espera = props.getEsperaRetryMs() * tentativa;
                log.warn("Provedor {} indisponivel (tentativa {}/{}). Nova tentativa em {} ms.",
                        nomeProvedor(), tentativa, tentativas, espera);
                try {
                    Thread.sleep(espera);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ultimaFalha;
                }
            }
        }
        throw ultimaFalha;
    }

    /**
     * Decide se vale a pena tentar de novo. 429 (cota), 5xx (carga do
     * provedor) e timeout de socket sao transitorios; 400 e 401 significam
     * requisicao ou credencial errada, e repetir so gasta cota.
     */
    private static boolean transitorio(Exception e) {
        for (Throwable causa = e; causa != null; causa = causa.getCause()) {
            String msg = causa.getMessage();
            if (msg == null) {
                continue;
            }
            if (msg.contains("429") || msg.contains("Too Many Requests")
                    || msg.contains("RESOURCE_EXHAUSTED") || msg.contains("UNAVAILABLE")
                    || msg.contains("503") || msg.contains("502") || msg.contains("504")
                    || msg.contains("500")) {
                return true;
            }
            if (msg.contains("401") || msg.contains("403") || msg.contains("400")) {
                return false;
            }
        }
        return causa(e) instanceof java.net.SocketTimeoutException || causa(e) instanceof java.io.IOException;
    }

    private static Throwable causa(Exception e) {
        Throwable atual = e;
        while (atual.getCause() != null) {
            atual = atual.getCause();
        }
        return atual;
    }

    @SuppressWarnings("unchecked")
    private RespostaLlm interpretar(String jsonResposta) {
        if (jsonResposta == null || jsonResposta.isBlank()) {
            throw new LlmException("O provedor de IA devolveu resposta vazia");
        }

        Map<String, Object> raiz;
        try {
            raiz = objectMapper.readValue(jsonResposta, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new LlmException("Não foi possível ler a resposta do provedor de IA", e);
        }

        Object erro = raiz.get("error");
        if (erro != null) {
            throw new LlmException("Provedor de IA recusou a requisição: " + erro);
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) raiz.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new LlmException("Provedor de IA não retornou nenhuma escolha");
        }

        Map<String, Object> mensagem = (Map<String, Object>) choices.get(0).get("message");
        if (mensagem == null) {
            throw new LlmException("Provedor de IA não retornou mensagem");
        }

        String conteudo = mensagem.get("content") == null ? null : String.valueOf(mensagem.get("content"));
        List<ChamadaFerramenta> chamadas = new ArrayList<>();

        Object toolCalls = mensagem.get("tool_calls");
        if (toolCalls instanceof List<?> lista) {
            for (Object item : lista) {
                Map<String, Object> chamada = (Map<String, Object>) item;
                Map<String, Object> funcao = (Map<String, Object>) chamada.get("function");
                if (funcao == null) {
                    continue;
                }
                chamadas.add(new ChamadaFerramenta(
                        chamada.get("id") == null ? null : String.valueOf(chamada.get("id")),
                        String.valueOf(funcao.get("name")),
                        lerArgumentos(funcao.get("arguments")),
                        lerThoughtSignature(chamada)));
            }
        }

        log.debug("Resposta do modelo: {} chamada(s) de ferramenta", chamadas.size());
        return new RespostaLlm(conteudo, chamadas, chamadas.isEmpty());
    }

    /**
     * Extrai a assinatura que o Gemini devolve em
     * {@code extra_content.google.thought_signature}. Sem ela, o Gemini recusa
     * a conversa no turno seguinte com HTTP 400, porque a chamada de função
     * chega "sem assinatura". Provedores OpenAI-padrão não têm esse campo e
     * devolvem {@code null}, que é o caso comum.
     */
    private static String lerThoughtSignature(Map<String, Object> chamada) {
        Object extra = chamada.get("extra_content");
        if (!(extra instanceof Map<?, ?> mapaExtra)) {
            return null;
        }
        Object google = mapaExtra.get("google");
        if (!(google instanceof Map<?, ?> mapaGoogle)) {
            return null;
        }
        Object assinatura = mapaGoogle.get("thought_signature");
        if (assinatura == null) {
            return null;
        }
        String texto = String.valueOf(assinatura);
        return texto.isBlank() ? null : texto;
    }

    private Map<String, Object> lerArgumentos(Object bruto) {
        if (bruto == null) {
            return Map.of();
        }
        if (bruto instanceof Map<?, ?> mapa) {
            return (Map<String, Object>) mapa;
        }
        try {
            return objectMapper.readValue(String.valueOf(bruto), new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new LlmException("Argumentos inválidos retornados pelo modelo: " + bruto, e);
        }
    }
}
