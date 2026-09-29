package sgs_barber.service;

import sgs_barber.dto.ClienteDTO;
import sgs_barber.model.Cliente;
import sgs_barber.model.TipoUsuario;
import sgs_barber.model.Usuario;
import sgs_barber.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public List<ClienteDTO> listarTodos() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ClienteDTO buscarPorId(Long id) {
        Cliente cliente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado com ID: " + id));
        return toDTO(cliente);
    }

    public ClienteDTO salvar(ClienteDTO dto) {
        Cliente cliente = toEntity(dto);
        return toDTO(repository.save(cliente));
    }

    public ClienteDTO atualizar(Long id, ClienteDTO dto) {
        Cliente cliente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado com ID: " + id));
        cliente.setNome(dto.getNome());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        sincronizarUsuario(cliente);
        return toDTO(repository.save(cliente));
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }

    private ClienteDTO toDTO(Cliente cliente) {
        ClienteDTO dto = new ClienteDTO();
        dto.setId(cliente.getId());
        dto.setNome(cliente.getNome());
        dto.setEmail(cliente.getEmail());
        dto.setTelefone(cliente.getTelefone());
        return dto;
    }

    private Cliente toEntity(ClienteDTO dto) {
        Cliente cliente = new Cliente();
        cliente.setId(dto.getId());
        cliente.setNome(dto.getNome());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        sincronizarUsuario(cliente);
        return cliente;
    }

    private void sincronizarUsuario(Cliente cliente) {
        Usuario usuario = cliente.getUsuario();
        if (usuario == null) {
            usuario = new Usuario();
            usuario.setTipo(TipoUsuario.CLIENTE);
            cliente.setUsuario(usuario);
        }
        usuario.setNome(cliente.getNome());
        usuario.setEmail(cliente.getEmail());
        usuario.setTelefone(cliente.getTelefone());
    }
}