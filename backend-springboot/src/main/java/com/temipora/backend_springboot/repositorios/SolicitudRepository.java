package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Solicitud;

import jakarta.persistence.LockModeType;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    Optional<Solicitud> findByIdSolicitud(Long idSolicitud);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Solicitud s WHERE s.idSolicitud = :idSolicitud")
    Optional<Solicitud> findByIdSolicitudForUpdate(@Param("idSolicitud") Long idSolicitud);

    @EntityGraph(attributePaths = { "servicio", "cliente", "artista", "moneda" })
    List<Solicitud> findByArtistaIdUsuarioOrderByFechaCreacionDesc(Long idArtista);

    @EntityGraph(attributePaths = { "servicio", "cliente", "artista", "moneda" })
    List<Solicitud> findByArtistaIdUsuarioAndEstadoOrderByFechaCreacionDesc(
            Long idArtista, EstadoSolicitud estado);

    @EntityGraph(attributePaths = { "servicio", "cliente", "artista", "moneda" })
    List<Solicitud> findByClienteIdUsuarioOrderByFechaCreacionDesc(Long idCliente);

    @EntityGraph(attributePaths = { "servicio", "cliente", "artista", "moneda" })
    List<Solicitud> findByClienteIdUsuarioAndEstadoOrderByFechaCreacionDesc(
            Long idCliente, EstadoSolicitud estado);

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
