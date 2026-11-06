package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoSustitucion;
import com.temipora.backend_springboot.modelo.Sustitucion;

public interface SustitucionRepository extends JpaRepository<Sustitucion, Long> {

    @Query("""
        SELECT s FROM Sustitucion s
         WHERE s.cliente.idUsuario = :idCliente
           AND s.servicioOrigen.idServicio = :idServicio
           AND s.estado = :estado
           AND s.fechaCreacion >= :desde
         ORDER BY s.fechaCreacion DESC
        """)
    List<Sustitucion> recientes(
            @Param("idCliente") Long idCliente,
            @Param("idServicio") Long idServicio,
            @Param("estado") EstadoSustitucion estado,
            @Param("desde") LocalDateTime desde,
            Pageable pageable);

    long countByEstado(EstadoSustitucion estado);

    @Query("""
        SELECT s.servicioOrigen.idServicio, s.servicioOrigen.nombre, COUNT(s)
          FROM Sustitucion s
         GROUP BY s.servicioOrigen.idServicio, s.servicioOrigen.nombre
         ORDER BY COUNT(s) DESC
        """)
    List<Object[]> origenesFrecuentes(Pageable pageable);
}
