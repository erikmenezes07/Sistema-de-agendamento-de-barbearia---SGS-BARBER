package sgs_barber.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sgs_barber.model.Barbeiro;

import java.util.List;
import java.util.Optional;

@Repository
public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {

    List<Barbeiro> findByAtivoTrueOrderByNomeAsc();

    List<Barbeiro> findByNomeContainingIgnoreCase(String nome);

    /**
     * Trava pessimista na linha do barbeiro durante a criacao do agendamento.
     * Sem isso, dois "criar_agendamento" simultaneos para o mesmo barbeiro
     * poderiam passar pela checagem de conflito ao mesmo tempo e gerar
     * horario duplicado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Barbeiro b where b.id = :id")
    Optional<Barbeiro> findByIdComLock(@Param("id") Long id);

    /**
     * Busca por especialidade usada pelo Agente Recomendador ("quem faz barba?").
     * Compara sem acento e sem diferenciar maiuscula/minuscula.
     */
    @Query("""
            select b from Barbeiro b
            where b.ativo = true
              and (lower(b.especialidades) like lower(concat('%', :termo, '%'))
                   or lower(b.nome) like lower(concat('%', :termo, '%')))
            order by b.nome
            """)
    List<Barbeiro> buscarPorEspecialidade(@Param("termo") String termo);
}
