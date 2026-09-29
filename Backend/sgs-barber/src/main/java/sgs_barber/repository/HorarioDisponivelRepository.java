package sgs_barber.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sgs_barber.model.HorarioDisponivel;

@Repository
public interface HorarioDisponivelRepository extends JpaRepository<HorarioDisponivel, Long> {
}