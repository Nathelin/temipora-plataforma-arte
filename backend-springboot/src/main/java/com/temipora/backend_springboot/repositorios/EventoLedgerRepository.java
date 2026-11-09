package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EventoLedger;

public interface EventoLedgerRepository extends JpaRepository<EventoLedger, Long> {

    List<EventoLedger> findByVarianteIdVarianteOrderBySecuenciaAsc(Long idVariante);

    Optional<EventoLedger> findTopByVarianteIdVarianteOrderBySecuenciaDesc(Long idVariante);

    boolean existsByVarianteIdVariante(Long idVariante);

    long countByVarianteIdVariante(Long idVariante);

    @Query("""
        SELECT DISTINCT e.variante.idVariante
          FROM EventoLedger e
         WHERE e.variante.producto.idProducto IN :idsProducto
        """)
    Set<Long> findVariantesConLedger(@Param("idsProducto") Collection<Long> idsProducto);
}
