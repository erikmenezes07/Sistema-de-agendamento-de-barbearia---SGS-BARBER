package com.sgsbarber.service;

import com.sgsbarber.model.ClientModel;
import com.sgsbarber.repository.ClientRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {
    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<ClientModel> listAll() { return clientRepository.findAll(); }
    public ClientModel findById(@NonNull Long id) { return clientRepository.findById(id).orElseThrow(); }
    public ClientModel create(@NonNull ClientModel client) { return clientRepository.save(client); }
    public ClientModel update(@NonNull Long id, @NonNull ClientModel client) {
        client.setId(id);
        return clientRepository.save(client);
    }
    public void delete(@NonNull Long id) { clientRepository.deleteById(id); }
}
