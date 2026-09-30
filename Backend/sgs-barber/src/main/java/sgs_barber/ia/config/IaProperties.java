package sgs_barber.ia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da integração com o modelo de linguagem.
 *
 * Tudo é lido de variáveis de ambiente para que a chave da IA nunca entre no
 * código (ver README). A implementação fala o formato /chat/completions da
 * OpenAI, então o mesmo código funciona com OpenAI, OpenRouter, Groq,
 * Together, Ollama e Gemini (endpoint compatível).
 */
@ConfigurationProperties(prefix = "ia")
public class IaProperties {

    /** Desliga a chamada externa (útil em desenvolvimento e nos testes). */
    private boolean habilitada = true;

    private String apiKey = "";

    private String baseUrl = "https://api.openai.com/v1";

    private String modelo = "gpt-4o-mini";

    private Double temperatura = 0.2;

    /** Timeout de conexão com o provedor, em milissegundos. */
    private int timeoutMs = 30000;

    /**
     * Quantas vezes repetir a chamada quando o provedor responde 429/5xx.
     * O plano gratuito derruba requisição grande por alguns segundos, então
     * repetir é melhor do que cair no agente de regras.
     */
    private int maxTentativas = 3;

    /** Espera base entre as retentativas, em milissegundos. */
    private long esperaRetryMs = 2500L;

    /**
     * Teto de tokens de resposta. Resposta de chat não precisa de mais que isso
     * e um teto curto segura o custo (e a cota do plano gratuito) quando o
     * modelo sai divagando.
     */
    private int maxTokens = 700;

    /**
     * Quantas rodadas de function calling são permitidas por mensagem do
     * cliente. Limita o custo e evita laço infinito quando o modelo insiste
     * em chamar ferramenta.
     */
    private int maxIteracoes = 6;

    /** Quantas mensagens do histórico são reenviadas ao modelo. */
    private int maxMensagensNoContexto = 20;

    /**
     * Quando não há chave configurada o sistema não quebra: responde com um
     * agente local baseado em regras, para a demonstração da arquitetura
     * funcionar mesmo sem orçamento de API.
     */
    private boolean permitirFallbackSemChave = true;

    /**
     * Liga ou desliga a integração externa.
     *
     * Com {@code true} (padrão), o front fala com {@code POST /api/chat} e o
     * modelo decide as tools. Com {@code false}, o front esconde a aba do
     * assistente e o projeto volta ao CRUD tradicional, sem nenhuma chamada de
     * IA — útil em deploy sem orçamento de API.
     */
    private boolean agendadorAtivo = true;

    public boolean isHabilitada() {
        return habilitada;
    }

    public void setHabilitada(boolean habilitada) {
        this.habilitada = habilitada;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Double temperatura) {
        this.temperatura = temperatura;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getMaxTentativas() {
        return maxTentativas;
    }

    public void setMaxTentativas(int maxTentativas) {
        this.maxTentativas = maxTentativas;
    }

    public long getEsperaRetryMs() {
        return esperaRetryMs;
    }

    public void setEsperaRetryMs(long esperaRetryMs) {
        this.esperaRetryMs = esperaRetryMs;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getMaxIteracoes() {
        return maxIteracoes;
    }

    public void setMaxIteracoes(int maxIteracoes) {
        this.maxIteracoes = maxIteracoes;
    }

    public int getMaxMensagensNoContexto() {
        return maxMensagensNoContexto;
    }

    public void setMaxMensagensNoContexto(int maxMensagensNoContexto) {
        this.maxMensagensNoContexto = maxMensagensNoContexto;
    }

    public boolean isPermitirFallbackSemChave() {
        return permitirFallbackSemChave;
    }

    public void setPermitirFallbackSemChave(boolean permitirFallbackSemChave) {
        this.permitirFallbackSemChave = permitirFallbackSemChave;
    }

    public boolean isAgendadorAtivo() {
        return agendadorAtivo;
    }

    public void setAgendadorAtivo(boolean agendadorAtivo) {
        this.agendadorAtivo = agendadorAtivo;
    }

    public boolean temChaveConfigurada() {
        return apiKey != null && !apiKey.isBlank();
    }
}
