package sgs_barber.repository;

import sgs_barber.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByNomeContainingIgnoreCase(String nome);

    List<Servico> findByNomeInIgnoreCase(List<String> nomes);

    /**
     * Busca textual (nome + descricao) usada pelo Agente Recomendador para
     * mapear uma preferencia em linguagem natural ("quero o cabelo mais curto")
     * para os servicos cadastrados.
     */
    @Query("""
            select s from Servico s
            where lower(s.nome) like lower(concat('%', :termo, '%'))
               or lower(coalesce(s.descricao, '')) like lower(concat('%', :termo, '%'))
            order by s.nome
            """)
    List<Servico> buscarPorTexto(@Param("termo") String termo);
}
