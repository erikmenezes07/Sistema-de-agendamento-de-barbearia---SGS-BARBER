package sgs_barber.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sgs_barber.model.Conversa;

import java.util.Optional;

@Repository
public interface ConversaRepository extends JpaRepository<Conversa, Long> {

    Optional<Conversa> findFirstByUsuarioIdOrderByAtualizadaEmDesc(String usuarioId);
}
