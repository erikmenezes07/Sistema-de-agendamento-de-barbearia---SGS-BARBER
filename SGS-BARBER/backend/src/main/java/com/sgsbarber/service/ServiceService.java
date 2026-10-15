package com.sgsbarber.service;

import com.sgsbarber.model.ServiceModel;
import com.sgsbarber.repository.ServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

import java.util.List;

@Service
public class ServiceService {

    private final ServiceRepository serviceRepository;

    public ServiceService(ServiceRepository serviceRepository) { this.serviceRepository = serviceRepository; }

    public List<ServiceModel> listAll() {
        return serviceRepository.findAll();
    }

    public ServiceModel findById(@NonNull Long id) { return serviceRepository.findById(id).orElseThrow(); }

    public ServiceModel create(@NonNull ServiceModel service) {
        return serviceRepository.save(service);
    }

    public ServiceModel update(@NonNull Long id, @NonNull ServiceModel service) {
        service.setId(id);
        return serviceRepository.save(service);
    }

    public void delete(@NonNull Long id) { serviceRepository.deleteById(id); }
}
