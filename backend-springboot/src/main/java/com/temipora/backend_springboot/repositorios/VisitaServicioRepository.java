package com.temipora.backend_springboot.repositorios;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.VisitaServicio;
import com.temipora.backend_springboot.modelo.VisitaServicioId;

public interface VisitaServicioRepository
        extends JpaRepository<VisitaServicio, VisitaServicioId> {

    @Modifying
    @Query(value = """
        INSERT INTO visita_servicio (id_usuario, id_servicio, fecha, cantidad, ultima_visita)
        VALUES (:idUsuario, :idServicio, CURRENT_DATE, 1, CURRENT_TIMESTAMP)
        ON CONFLICT (id_usuario, id_servicio, fecha)
        DO UPDATE SET cantidad = visita_servicio.cantidad + 1,
                      ultima_visita = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void registrar(
        @Param("idUsuario") Long idUsuario,
        @Param("idServicio") Long idServicio);

    @Query(value = """
        SELECT id_servicio, SUM(cantidad), MAX(ultima_visita)
        FROM visita_servicio
        WHERE id_usuario = :idUsuario
          AND fecha >= :desde
        GROUP BY id_servicio
        """, nativeQuery = true)
    List<Object[]> resumenPorUsuario(
        @Param("idUsuario") Long idUsuario,
        @Param("desde") LocalDate desde);

    @Query(value = """
        SELECT DISTINCT se.id_etiqueta
        FROM visita_servicio vs
        JOIN servicio_etiqueta se ON se.id_servicio = vs.id_servicio
        WHERE vs.id_usuario = :idUsuario
          AND vs.fecha >= :desde
        """, nativeQuery = true)
    List<Long> etiquetasVisitadas(
        @Param("idUsuario") Long idUsuario,
        @Param("desde") LocalDate desde);
}
