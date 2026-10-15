package com.sgsbarber.repository;

import com.sgsbarber.model.BarberModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BarberRepository extends JpaRepository<BarberModel, Long> {
}