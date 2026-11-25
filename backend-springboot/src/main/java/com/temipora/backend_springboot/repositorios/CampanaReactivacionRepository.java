package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.CampanaReactivacion;
import com.temipora.backend_springboot.modelo.EstadoCampanaReactivacion;

import jakarta.persistence.LockModeType;

public interface CampanaReactivacionRepository
        extends JpaRepository<CampanaReactivacion, Long> {

    Optional<CampanaReactivacion> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CampanaReactivacion c WHERE c.token = :token")
    Optional<CampanaReactivacion> findByTokenParaActualizar(
        @Param("token") String token);

    @Query("""
        SELECT DISTINCT c FROM CampanaReactivacion c
        JOIN FETCH c.usuario
        LEFT JOIN FETCH c.ofertas o
        LEFT JOIN FETCH o.servicio s
        LEFT JOIN FETCH s.artista
        WHERE c.token = :token
        """)
    Optional<CampanaReactivacion> findByTokenConOfertas(@Param("token") String token);

    @Query("""
        SELECT DISTINCT c FROM CampanaReactivacion c
        LEFT JOIN FETCH c.ofertas o
        LEFT JOIN FETCH o.servicio s
        LEFT JOIN FETCH s.artista
        WHERE c.usuario.idUsuario = :idUsuario
          AND c.estado = :estado
        ORDER BY c.fechaCreacion DESC
        """)
    List<CampanaReactivacion> findConOfertasPorUsuario(
        @Param("idUsuario") Long idUsuario,
        @Param("estado") EstadoCampanaReactivacion estado);

    List<CampanaReactivacion> findByEstadoAndFechaVencimientoBefore(
        EstadoCampanaReactivacion estado,
        LocalDateTime fecha);

    @Query("""
        SELECT COUNT(c) FROM CampanaReactivacion c
        WHERE c.fechaCreacion >= :desde
          AND c.fechaCreacion < :hasta
        """)
    long contarEntre(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);
}
