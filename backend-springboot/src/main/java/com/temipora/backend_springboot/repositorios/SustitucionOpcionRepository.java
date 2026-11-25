package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.SustitucionOpcion;

public interface SustitucionOpcionRepository
        extends JpaRepository<SustitucionOpcion, Long> {

    List<SustitucionOpcion> findBySustitucionIdSustitucionOrderByOrdenAsc(
            Long idSustitucion);

    Optional<SustitucionOpcion> findBySustitucionIdSustitucionAndServicioIdServicio(
            Long idSustitucion, Long idServicio);

    @Query("""
        SELECT o FROM SustitucionOpcion o
        JOIN FETCH o.servicio s
        JOIN FETCH s.artista
        JOIN FETCH o.artista
        JOIN FETCH o.moneda
        WHERE o.sustitucion.idSustitucion = :idSustitucion
        ORDER BY o.orden ASC
        """)
    List<SustitucionOpcion> findHistorialPorSustitucion(
        @Param("idSustitucion") Long idSustitucion);
}
