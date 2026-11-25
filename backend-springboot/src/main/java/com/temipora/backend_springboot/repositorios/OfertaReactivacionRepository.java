package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoOfertaReactivacion;
import com.temipora.backend_springboot.modelo.OfertaReactivacion;

public interface OfertaReactivacionRepository
        extends JpaRepository<OfertaReactivacion, Long> {

    Optional<OfertaReactivacion> findByIdOferta(Long idOferta);

    @Query("""
        SELECT o FROM OfertaReactivacion o
        JOIN FETCH o.campana c
        JOIN FETCH o.servicio s
        JOIN FETCH s.artista
        WHERE o.idOferta = :idOferta
        """)
    Optional<OfertaReactivacion> findByIdConDetalle(
        @Param("idOferta") Long idOferta);

    @Query("""
        SELECT o FROM OfertaReactivacion o
        JOIN FETCH o.campana c
        JOIN FETCH o.servicio s
        JOIN FETCH s.artista
        WHERE c.usuario.idUsuario = :idUsuario
          AND o.estado IN :estados
          AND o.fechaVencimiento > :ahora
        ORDER BY o.fechaVencimiento ASC, o.idOferta ASC
        """)
    List<OfertaReactivacion> findActivasPorUsuario(
        @Param("idUsuario") Long idUsuario,
        @Param("estados") Collection<EstadoOfertaReactivacion> estados,
        @Param("ahora") LocalDateTime ahora);

    @Query("""
        SELECT o FROM OfertaReactivacion o
        JOIN FETCH o.campana c
        JOIN FETCH o.servicio s
        JOIN FETCH s.artista
        WHERE c.token = :token
        ORDER BY o.idOferta ASC
        """)
    List<OfertaReactivacion> findByTokenCampana(@Param("token") String token);

    @Query("""
        SELECT o FROM OfertaReactivacion o
        JOIN FETCH o.servicio s
        WHERE o.campana.usuario.idUsuario = :idUsuario
          AND s.idServicio IN :ids
          AND o.estado IN :estados
          AND o.fechaVencimiento > :ahora
        """)
    List<OfertaReactivacion> findVigentesPorUsuarioYServicios(
        @Param("idUsuario") Long idUsuario,
        @Param("ids") Collection<Long> ids,
        @Param("estados") Collection<EstadoOfertaReactivacion> estados,
        @Param("ahora") LocalDateTime ahora);

    boolean existsByCampanaIdCampanaAndEstado(
        Long idCampana,
        EstadoOfertaReactivacion estado);

    @Query("""
        SELECT o.estado, COUNT(o) FROM OfertaReactivacion o
        WHERE o.fechaCreacion >= :desde
          AND o.fechaCreacion < :hasta
        GROUP BY o.estado
        """)
    List<Object[]> contarPorEstadoEntre(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);

    @Query("""
        SELECT COUNT(o) FROM OfertaReactivacion o
        WHERE o.fechaCreacion >= :desde
          AND o.fechaCreacion < :hasta
          AND o.fechaReserva IS NOT NULL
        """)
    long contarAceptadasEntre(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);
}
