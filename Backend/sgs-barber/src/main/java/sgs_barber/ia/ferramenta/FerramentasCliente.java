package sgs_barber.ia.ferramenta;

import org.springframework.stereotype.Component;
import sgs_barber.ia.util.Argumentos;
import sgs_barber.ia.util.Texto;
import sgs_barber.model.Cliente;
import sgs_barber.model.TipoAgente;
import sgs_barber.repository.ClienteRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static sgs_barber.ia.ferramenta.Esquema.objeto;
import static sgs_barber.ia.ferramenta.Esquema.string;

/**
 * Tool de leitura de cliente. O Agente precisa do id do cliente para fechar um
 * agendamento, e o id quase nunca vem pronto na conversa.
 */
@Component
public class FerramentasCliente implements ConjuntoDeFerramentas {

    public static final String BUSCAR_CLIENTE = "buscar_cliente";

    private final ClienteRepository repository;

    public FerramentasCliente(ClienteRepository repository) {
        this.repository = repository;
    }

    @Override
    public void registrar(RegistroFerramentas registro) {
        registro.add(new Ferramenta(
                BUSCAR_CLIENTE,
                "Procura um cliente por nome, telefone ou email e devolve o id, "
                        + "necessario para criar um agendamento.",
                objeto(string("termo", "Nome, telefone ou email do cliente.", true)),
                Set.of(TipoAgente.AGENDADOR),
                this::buscar));
    }

    private Object buscar(Map<String, Object> args) {
        String termo = Argumentos.textoObrigatorio(args, "termo");
        String alvo = Texto.normalizar(termo);
        String digitos = termo.replaceAll("\\D+", "");

        List<Cliente> encontrados = new ArrayList<>();
        for (Cliente c : repository.findAll()) {
            boolean bate = Texto.normalizar(c.getNome()).contains(alvo)
                    || Texto.normalizar(c.getEmail()).contains(alvo)
                    || (!digitos.isEmpty() && c.getTelefone().replaceAll("\\D+", "").contains(digitos));
            if (bate) {
                encontrados.add(c);
            }
        }

        List<Map<String, Object>> resposta = new ArrayList<>();
        for (Cliente c : encontrados) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("nome", c.getNome());
            item.put("email", c.getEmail());
            item.put("telefone", c.getTelefone());
            resposta.add(item);
        }

        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("quantidade", resposta.size());
        saida.put("clientes", resposta);
        if (encontrados.isEmpty()) {
            saida.put("aviso", "Nenhum cliente encontrado para '" + termo + "'. "
                    + "Peca nome, telefone e email para cadastrar antes de agendar.");
        }
        return saida;
    }
}
