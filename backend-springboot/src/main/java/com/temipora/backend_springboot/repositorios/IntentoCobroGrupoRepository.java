package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobroGrupo;

public interface IntentoCobroGrupoRepository
        extends JpaRepository<IntentoCobroGrupo, Long> {

    List<IntentoCobroGrupo> findByIntentoCobroIdIntentoCobro(Long idIntentoCobro);

    Optional<IntentoCobroGrupo> findByIntentoCobroIdIntentoCobroAndGrupoIdCompraGrupo(
            Long idIntentoCobro, Long idCompraGrupo);

    Optional<IntentoCobroGrupo> findFirstByGrupoIdCompraGrupoAndEstadoOrderByIntentoCobroFechaCreacionDesc(
            Long idCompraGrupo, EstadoIntentoCobro estado);
}
