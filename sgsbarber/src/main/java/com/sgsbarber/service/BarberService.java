package com.sgsbarber.service;

import com.sgsbarber.model.BarberModel;
import com.sgsbarber.repository.BarberRepository;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

import java.util.List;

@Service
public class BarberService {

    private final BarberRepository barberRepository;

    public BarberService(BarberRepository barberRepository) {
        this.barberRepository = barberRepository;
    }

    public List<BarberModel> listAll() {
        return barberRepository.findAll();
    }

    public BarberModel findById(@NonNull Long id) { return barberRepository.findById(id).orElseThrow(); }

    public BarberModel create(@NonNull BarberModel barber) {
        return barberRepository.save(barber);
    }

    public BarberModel update(@NonNull Long id, @NonNull BarberModel barber) {
        barber.setId(id);
        return barberRepository.save(barber);
    }

    public void delete(@NonNull Long id) { barberRepository.deleteById(id); }
}
