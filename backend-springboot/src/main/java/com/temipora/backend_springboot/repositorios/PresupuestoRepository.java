package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Presupuesto;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    Optional<Presupuesto> findBySolicitudIdSolicitud(Long idSolicitud);

    boolean existsBySolicitudIdSolicitud(Long idSolicitud);

    @Query("""
        SELECT p
        FROM Presupuesto p
        JOIN FETCH p.solicitud s
        WHERE s.estado = :estado
          AND p.fechaVencimiento < :ahora
        ORDER BY p.fechaVencimiento ASC
        """)
    List<Presupuesto> findVencidos(
            @Param("ahora") LocalDateTime ahora,
            @Param("estado") EstadoSolicitud estado,
            Pageable pageable);
}
