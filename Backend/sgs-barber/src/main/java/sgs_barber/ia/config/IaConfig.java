package sgs_barber.ia.config;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sgs_barber.ia.LlmClient;
import sgs_barber.ia.RestClientLlmClient;

@Configuration
@EnableConfigurationProperties(IaProperties.class)
public class IaConfig {

    /**
     * O cliente HTTP só é criado quando existe chave configurada
     * (IA_API_KEY). Sem chave, o orquestrador cai no agente local baseado em
     * regras em vez de falhar no boot.
     *
     * No orquestrador a injeção é feita com {@link ObjectProvider}, porque
     * nesse cenário o bean simplesmente não existe.
     */
    @Bean
    @ConditionalOnProperty(prefix = "ia", name = "api-key")
    public LlmClient llmClient(IaProperties props, ObjectMapper objectMapper) {
        return new RestClientLlmClient(props, objectMapper);
    }
}
