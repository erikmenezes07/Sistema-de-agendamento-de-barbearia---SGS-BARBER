package sgs_barber.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS do modulo de IA.
 *
 * O front do SGS-BARBER e HTML puro aberto no navegador (Live Server, file://
 * ou servido por qualquer porta), enquanto a API roda no Spring Boot. Como a
 * origem muda conforme a maquina, listar portas fixas quebraria em silencio: o
 * navegador bloqueia e o chat simplesmente nao responde.
 *
 * Por isso o default libera apenas loopback, que e o cenario de
 * desenvolvimento. Em deploy, defina {@code sgs.cors.origins-autorizadas}
 * com os dominios reais — em especial se Allow-Credentials estiver ligado,
 * porque "origem larga + credenciais" e exactement o que o CORS proibe.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String origensAutorizadas;

    public WebConfig(@Value("${sgs.cors.origens-autorizadas:}") String origensAutorizadas) {
        this.origensAutorizadas = origensAutorizadas;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origensAutorizadas.isBlank() ? padroesDeDesenvolvimento() : origensAutorizadas.split("\\s*,\\s*"))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    private String[] padroesDeDesenvolvimento() {
        return new String[]{
                "http://localhost:*",
                "http://127.0.0.1:*",
                "file://"
        };
    }
}
