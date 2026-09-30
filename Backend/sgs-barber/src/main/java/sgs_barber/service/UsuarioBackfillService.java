package sgs_barber.service;

import jakarta.transaction.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import sgs_barber.model.Barbeiro;
import sgs_barber.model.Cliente;
import sgs_barber.model.TipoUsuario;
import sgs_barber.model.Usuario;
import sgs_barber.repository.BarbeiroRepository;
import sgs_barber.repository.ClienteRepository;

@Service
public class UsuarioBackfillService {

    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;

    public UsuarioBackfillService(BarbeiroRepository barbeiroRepository,
            ClienteRepository clienteRepository) {
        this.barbeiroRepository = barbeiroRepository;
        this.clienteRepository = clienteRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void preencherUsuariosExistentes() {
        barbeiroRepository.findAll().stream()
                .filter(barbeiro -> barbeiro.getUsuario() == null)
                .forEach(this::criarUsuarioPara);

        clienteRepository.findAll().stream()
                .filter(cliente -> cliente.getUsuario() == null)
                .forEach(this::criarUsuarioPara);
    }

    private void criarUsuarioPara(Barbeiro barbeiro) {
        Usuario usuario = new Usuario();
        usuario.setNome(barbeiro.getNome());
        usuario.setTelefone(barbeiro.getTelefone());
        usuario.setTipo(TipoUsuario.BARBEIRO);
        barbeiro.setUsuario(usuario);
        barbeiroRepository.save(barbeiro);
    }

    private void criarUsuarioPara(Cliente cliente) {
        Usuario usuario = new Usuario();
        usuario.setNome(cliente.getNome());
        usuario.setEmail(cliente.getEmail());
        usuario.setTelefone(cliente.getTelefone());
        usuario.setTipo(TipoUsuario.CLIENTE);
        cliente.setUsuario(usuario);
        clienteRepository.save(cliente);
    }
}