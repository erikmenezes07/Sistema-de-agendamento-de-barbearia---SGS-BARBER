package sgs_barber.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sgs_barber.model.HorarioBarbeiro;

import java.util.List;

@Repository
public interface HorarioBarbeiroRepository extends JpaRepository<HorarioBarbeiro, Long> {

    List<HorarioBarbeiro> findByBarbeiroId(Long barbeiroId);

    List<HorarioBarbeiro> findByBarbeiroIdAndDiaSemana(Long barbeiroId, Integer diaSemana);
}
