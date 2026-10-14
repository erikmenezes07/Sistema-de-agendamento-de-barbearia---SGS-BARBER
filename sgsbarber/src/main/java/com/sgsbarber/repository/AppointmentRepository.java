package com.sgsbarber.repository;

import com.sgsbarber.model.AppointmentModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<AppointmentModel, Long> {
    List<AppointmentModel> findByDate(LocalDate date);
    List<AppointmentModel> findByBarberIdAndDate(Long barberId, LocalDate date);
}
