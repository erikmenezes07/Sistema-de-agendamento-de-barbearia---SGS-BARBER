package com.sgsbarber.repository;

import com.sgsbarber.model.BarberScheduleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BarberScheduleRepository extends JpaRepository<BarberScheduleModel, Long> {
    List<BarberScheduleModel> findByBarberIdAndWeekday(Long barberId, Short weekday);
}
