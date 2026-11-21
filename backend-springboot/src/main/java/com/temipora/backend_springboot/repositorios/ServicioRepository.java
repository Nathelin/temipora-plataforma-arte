package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Servicio;

import jakarta.persistence.LockModeType;

public interface ServicioRepository extends JpaRepository<Servicio, Long>, JpaSpecificationExecutor<Servicio> {

    Optional<Servicio> findByIdServicio(Long idServicio);

    @EntityGraph(attributePaths = {
        "artista",
        "tipoServicio",
        "medio",
        "moneda",
        "imagenes",
        "etiquetas",
        "etiquetas.etiqueta",
        "etiquetas.etiqueta.categoria"
    })
    List<Servicio> findByIdServicioIn(Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Servicio s WHERE s.idServicio = :idServicio")
    Optional<Servicio> findByIdServicioForUpdate(@Param("idServicio") Long idServicio);

    @EntityGraph(attributePaths = {
        "artista",
        "imagenes",
        "etiquetas",
        "etiquetas.etiqueta",
        "etiquetas.etiqueta.categoria"
    })
    List<Servicio> findByArtistaIdUsuarioAndEstadoNotOrderByNombreAsc(
            Long idUsuario, EstadoServicio estado);

    @EntityGraph(attributePaths = {
        "artista",
        "tipoServicio",
        "medio",
        "moneda",
        "imagenes",
        "etiquetas",
        "etiquetas.etiqueta",
        "etiquetas.etiqueta.categoria"
    })
    List<Servicio> findByArtistaIdUsuarioInAndEstado(
            Collection<Long> idsArtista, EstadoServicio estado);

    List<Servicio> findByEstadoOrderByArchivadoEnDesc(EstadoServicio estado);

    List<Servicio> findByArtistaIdUsuarioAndEstado(
            Long idUsuario, EstadoServicio estado);

    @Query("SELECT s.imagenPortada FROM Servicio s WHERE s.imagenPortada IS NOT NULL")
    List<String> findImagenesPortada();

    boolean existsByImagenPortada(String imagenPortada);

    boolean existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndEstadoNot(
            Long idUsuario, Long idTipoServicio, String nombre, EstadoServicio estado);

    boolean existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndIdServicioNotAndEstadoNot(
            Long idUsuario, Long idTipoServicio, String nombre, Long idServicio, EstadoServicio estado);

    /**
     * Servicios publicados como abiertos que ya no tienen cupo libre. El job
     * de reconciliación los pasa a lista de espera si un evento se perdió.
     */
    @Query("""
        SELECT s FROM Servicio s
         WHERE s.estado = :publicado
           AND s.modoRecepcion = :abierta
           AND s.cupos <= (SELECT COUNT(sol) FROM Solicitud sol
                            WHERE sol.servicio = s
                              AND sol.estado IN :estados)
        """)
    List<Servicio> buscarLlenos(
        @Param("publicado") EstadoServicio publicado,
        @Param("abierta") ModoRecepcionSolicitud abierta,
        @Param("estados") Collection<EstadoSolicitud> estados);

    /**
     * Servicios que el sistema pasó a lista de espera y que ya tienen cupo
     * libre otra vez.
     */
    @Query("""
        SELECT s FROM Servicio s
         WHERE s.estado = :publicado
           AND s.modoRecepcion = :espera
           AND s.modoRecepcionPreferido = :abierta
           AND s.cupos > (SELECT COUNT(sol) FROM Solicitud sol
                           WHERE sol.servicio = s
                             AND sol.estado IN :estados)
        """)
    List<Servicio> buscarReabribles(
        @Param("publicado") EstadoServicio publicado,
        @Param("espera") ModoRecepcionSolicitud espera,
        @Param("abierta") ModoRecepcionSolicitud abierta,
        @Param("estados") Collection<EstadoSolicitud> estados);
}
