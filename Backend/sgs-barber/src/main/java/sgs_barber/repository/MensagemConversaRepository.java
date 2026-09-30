package sgs_barber.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sgs_barber.model.MensagemConversa;

import java.util.List;

@Repository
public interface MensagemConversaRepository extends JpaRepository<MensagemConversa, Long> {

    List<MensagemConversa> findByConversaIdOrderByOrdemAsc(Long conversaId);

    void deleteByConversaId(Long conversaId);
}
