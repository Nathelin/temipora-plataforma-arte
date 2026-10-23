package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Solicitud;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    Optional<Solicitud> findByIdSolicitud(Long idSolicitud);

    boolean existsByServicioIdServicioAndClienteIdUsuarioAndEstadoIn(
            Long idServicio, Long idCliente, Collection<EstadoSolicitud> estados);

    boolean existsByServicioIdServicioAndClienteIdUsuarioAndEstado(
            Long idServicio, Long idCliente, EstadoSolicitud estado);

    long countByServicioIdServicioAndEstadoIn(
            Long idServicio, Collection<EstadoSolicitud> estados);

    @Query("""
        SELECT COALESCE(SUM(s.cantidadDibujos), 0)
        FROM Solicitud s
        WHERE s.servicio.idServicio = :idServicio
          AND s.estado IN :estados
        """)
    Long sumCantidadDibujosByServicioAndEstadoIn(
            @Param("idServicio") Long idServicio,
            @Param("estados") Collection<EstadoSolicitud> estados);
}
