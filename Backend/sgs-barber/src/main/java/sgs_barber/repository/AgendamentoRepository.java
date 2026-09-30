package sgs_barber.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sgs_barber.model.Agendamento;
import sgs_barber.model.StatusAgendamento;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findByClienteId(Long clienteId);

    List<Agendamento> findByBarbeiroIdAndDataAgendamento(Long barbeiroId, LocalDate data);

    // Usado na checagem de conflito: traz só os agendamentos que realmente "ocupam" a agenda
    List<Agendamento> findByBarbeiroIdAndDataAgendamentoAndStatusNot(
            Long barbeiroId, LocalDate data, StatusAgendamento status);
}
