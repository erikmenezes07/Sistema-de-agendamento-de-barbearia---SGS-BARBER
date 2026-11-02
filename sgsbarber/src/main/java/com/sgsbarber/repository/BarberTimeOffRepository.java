package com.sgsbarber.repository;

import com.sgsbarber.model.BarberTimeOffModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BarberTimeOffRepository extends JpaRepository<BarberTimeOffModel, Long> {
}
