package sgs_barber.ia.config;

import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sgs_barber.ia.ferramenta.ConjuntoDeFerramentas;
import sgs_barber.ia.ferramenta.RegistroFerramentas;

import java.util.List;

/**
 * Monta o catalogo de ferramentas a partir de todos os beans
 * {@link ConjuntoDeFerramentas} da aplicacao. Cada tool declara seus agentes
 * autorizados, entao adicionar uma tool e uma linha no componente que a
 * registra — nao ha lista central para manter sincronizada.
 */
@Configuration
public class FerramentasConfig {

    @Bean
    public RegistroFerramentas registroFerramentas(ObjectMapper objectMapper,
                                                    List<ConjuntoDeFerramentas> conjuntos) {
        RegistroFerramentas registro = new RegistroFerramentas(objectMapper);
        conjuntos.forEach(conjunto -> conjunto.registrar(registro));
        return registro;
    }
}
