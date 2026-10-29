package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.ReseniaVersion;

public interface ReseniaVersionRepository
        extends JpaRepository<ReseniaVersion, Long> {

    List<ReseniaVersion> findByReseniaIdReseniaOrderByNumeroAsc(Long idResenia);

    long countByReseniaIdResenia(Long idResenia);

    @Query("""
        SELECT v FROM ReseniaVersion v
        WHERE v.resenia.idResenia IN :ids
          AND v.numero = (
              SELECT MAX(v2.numero) FROM ReseniaVersion v2
              WHERE v2.resenia = v.resenia
          )
        """)
    List<ReseniaVersion> findActualesPorReseniaIds(
        @Param("ids") Collection<Long> ids);
}
