package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.SancionCuenta;

public interface SancionCuentaRepository
        extends JpaRepository<SancionCuenta, Long>,
                JpaSpecificationExecutor<SancionCuenta> {

    @Query("""
        SELECT s FROM SancionCuenta s
        WHERE s.usuario.idUsuario = :idUsuario
          AND s.estado = :estado
          AND (s.fechaFin IS NULL OR s.fechaFin > :ahora)
        ORDER BY s.fechaInicio DESC
        """)
    List<SancionCuenta> findVigentesByUsuarioId(
            @Param("idUsuario") Long idUsuario,
            @Param("estado") EstadoSancion estado,
            @Param("ahora") LocalDateTime ahora);

    @Query("""
        SELECT s FROM SancionCuenta s
        WHERE s.usuario.idUsuario IN :ids
          AND s.estado = :estado
          AND (s.fechaFin IS NULL OR s.fechaFin > :ahora)
        """)
    List<SancionCuenta> findVigentesByUsuarioIds(
            @Param("ids") Collection<Long> ids,
            @Param("estado") EstadoSancion estado,
            @Param("ahora") LocalDateTime ahora);

    @Query("""
        SELECT s FROM SancionCuenta s
        WHERE lower(s.usuario.nombreUsuario) = lower(:nombreUsuario)
          AND s.estado = :estado
          AND (s.fechaFin IS NULL OR s.fechaFin > :ahora)
        ORDER BY s.fechaInicio DESC
        """)
    List<SancionCuenta> findVigentesByNombreUsuario(
            @Param("nombreUsuario") String nombreUsuario,
            @Param("estado") EstadoSancion estado,
            @Param("ahora") LocalDateTime ahora);

    List<SancionCuenta> findByUsuarioIdUsuarioOrderByFechaInicioDesc(Long idUsuario);
}
