package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobroGrupo;

public interface IntentoCobroGrupoRepository
        extends JpaRepository<IntentoCobroGrupo, Long> {

    List<IntentoCobroGrupo> findByIntentoCobroIdIntentoCobro(Long idIntentoCobro);

    @Query("""
        SELECT ig FROM IntentoCobroGrupo ig
        JOIN FETCH ig.intentoCobro
        WHERE ig.grupo.idCompraGrupo IN :ids
          AND ig.estado = :estado
        """)
    List<IntentoCobroGrupo> buscarPorGrupos(
            @Param("ids") Collection<Long> ids,
            @Param("estado") EstadoIntentoCobro estado);

    Optional<IntentoCobroGrupo> findByIntentoCobroIdIntentoCobroAndGrupoIdCompraGrupo(
            Long idIntentoCobro, Long idCompraGrupo);

    Optional<IntentoCobroGrupo> findFirstByGrupoIdCompraGrupoAndEstadoOrderByIntentoCobroFechaCreacionDesc(
            Long idCompraGrupo, EstadoIntentoCobro estado);
}
