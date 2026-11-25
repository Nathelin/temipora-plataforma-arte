package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.DisparadorSustitucion;
import com.temipora.backend_springboot.modelo.EstadoSustitucion;
import com.temipora.backend_springboot.modelo.MotivoSustitucion;
import com.temipora.backend_springboot.modelo.Sustitucion;

public interface SustitucionRepository extends JpaRepository<Sustitucion, Long> {

    @Query("""
        SELECT s FROM Sustitucion s
         WHERE s.cliente.idUsuario = :idCliente
           AND s.servicioOrigen.idServicio = :idServicio
           AND s.motivo = :motivo
           AND s.estado = :estado
           AND s.fechaCreacion >= :desde
         ORDER BY s.fechaCreacion DESC
        """)
    List<Sustitucion> recientes(
            @Param("idCliente") Long idCliente,
            @Param("idServicio") Long idServicio,
            @Param("motivo") MotivoSustitucion motivo,
            @Param("estado") EstadoSustitucion estado,
            @Param("desde") LocalDateTime desde,
            Pageable pageable);

    long countByMotivoNot(MotivoSustitucion motivo);

    long countByEstadoAndMotivoNot(
        EstadoSustitucion estado,
        MotivoSustitucion motivo);

    @Query(value = """
        SELECT s FROM Sustitucion s
        LEFT JOIN FETCH s.cliente c
        JOIN FETCH s.servicioOrigen so
        JOIN FETCH s.artistaOrigen ao
        LEFT JOIN FETCH s.servicioElegido se
        LEFT JOIN FETCH s.solicitud sol
        LEFT JOIN FETCH sol.pedido
        LEFT JOIN FETCH s.pedido
        WHERE s.fechaCreacion >= :desde
          AND s.fechaCreacion < :hasta
          AND s.motivo <> :motivoExcluido
          AND (:estado IS NULL OR s.estado = :estado)
          AND (:motivo IS NULL OR s.motivo = :motivo)
          AND (:disparador IS NULL OR s.disparador = :disparador)
          AND (:patron = ''
               OR lower(c.nombreUsuario) LIKE concat('%', :patron, '%')
               OR lower(c.nombreDisplay) LIKE concat('%', :patron, '%')
               OR lower(c.nombre) LIKE concat('%', :patron, '%')
               OR lower(c.apellido) LIKE concat('%', :patron, '%')
               OR lower(so.nombre) LIKE concat('%', :patron, '%')
               OR lower(ao.nombreUsuario) LIKE concat('%', :patron, '%')
               OR lower(ao.nombreDisplay) LIKE concat('%', :patron, '%')
               OR lower(se.nombre) LIKE concat('%', :patron, '%'))
        """,
        countQuery = """
        SELECT count(s) FROM Sustitucion s
        LEFT JOIN s.cliente c
        JOIN s.servicioOrigen so
        JOIN s.artistaOrigen ao
        LEFT JOIN s.servicioElegido se
        WHERE s.fechaCreacion >= :desde
          AND s.fechaCreacion < :hasta
          AND s.motivo <> :motivoExcluido
          AND (:estado IS NULL OR s.estado = :estado)
          AND (:motivo IS NULL OR s.motivo = :motivo)
          AND (:disparador IS NULL OR s.disparador = :disparador)
          AND (:patron = ''
               OR lower(c.nombreUsuario) LIKE concat('%', :patron, '%')
               OR lower(c.nombreDisplay) LIKE concat('%', :patron, '%')
               OR lower(c.nombre) LIKE concat('%', :patron, '%')
               OR lower(c.apellido) LIKE concat('%', :patron, '%')
               OR lower(so.nombre) LIKE concat('%', :patron, '%')
               OR lower(ao.nombreUsuario) LIKE concat('%', :patron, '%')
               OR lower(ao.nombreDisplay) LIKE concat('%', :patron, '%')
               OR lower(se.nombre) LIKE concat('%', :patron, '%'))
        """)
    Page<Sustitucion> buscarHistorial(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        @Param("estado") EstadoSustitucion estado,
        @Param("motivo") MotivoSustitucion motivo,
        @Param("disparador") DisparadorSustitucion disparador,
        @Param("patron") String patron,
        @Param("motivoExcluido") MotivoSustitucion motivoExcluido,
        Pageable pageable);

    @Query("""
        SELECT s.estado, s.motivo, COUNT(s),
               COALESCE(SUM(s.cantidadOpciones), 0),
               COALESCE(SUM(CASE WHEN s.cliente IS NULL THEN 1 ELSE 0 END), 0)
        FROM Sustitucion s
        LEFT JOIN s.cliente c
        JOIN s.servicioOrigen so
        JOIN s.artistaOrigen ao
        LEFT JOIN s.servicioElegido se
        WHERE s.fechaCreacion >= :desde
          AND s.fechaCreacion < :hasta
          AND s.motivo <> :motivoExcluido
          AND (:estado IS NULL OR s.estado = :estado)
          AND (:motivo IS NULL OR s.motivo = :motivo)
          AND (:disparador IS NULL OR s.disparador = :disparador)
          AND (:patron = ''
               OR lower(c.nombreUsuario) LIKE concat('%', :patron, '%')
               OR lower(c.nombreDisplay) LIKE concat('%', :patron, '%')
               OR lower(c.nombre) LIKE concat('%', :patron, '%')
               OR lower(c.apellido) LIKE concat('%', :patron, '%')
               OR lower(so.nombre) LIKE concat('%', :patron, '%')
               OR lower(ao.nombreUsuario) LIKE concat('%', :patron, '%')
               OR lower(ao.nombreDisplay) LIKE concat('%', :patron, '%')
               OR lower(se.nombre) LIKE concat('%', :patron, '%'))
        GROUP BY s.estado, s.motivo
        ORDER BY COUNT(s) DESC
        """)
    List<Object[]> agruparHistorial(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        @Param("estado") EstadoSustitucion estado,
        @Param("motivo") MotivoSustitucion motivo,
        @Param("disparador") DisparadorSustitucion disparador,
        @Param("patron") String patron,
        @Param("motivoExcluido") MotivoSustitucion motivoExcluido);

    @Query("""
        SELECT s FROM Sustitucion s
        LEFT JOIN FETCH s.cliente
        JOIN FETCH s.servicioOrigen so
        JOIN FETCH s.artistaOrigen
        LEFT JOIN FETCH s.servicioElegido se
        LEFT JOIN FETCH se.artista
        LEFT JOIN FETCH s.solicitud sol
        LEFT JOIN FETCH sol.pedido
        LEFT JOIN FETCH s.pedido
        WHERE s.idSustitucion = :idSustitucion
        """)
    Optional<Sustitucion> findByIdConDetalle(
        @Param("idSustitucion") Long idSustitucion);
}
