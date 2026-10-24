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
import com.temipora.backend_springboot.modelo.Servicio;

import jakarta.persistence.LockModeType;

public interface ServicioRepository extends JpaRepository<Servicio, Long>, JpaSpecificationExecutor<Servicio> {

    Optional<Servicio> findByIdServicio(Long idServicio);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Servicio s WHERE s.idServicio = :idServicio")
    Optional<Servicio> findByIdServicioForUpdate(@Param("idServicio") Long idServicio);

    List<Servicio> findByArtistaIdUsuarioAndEstadoNotOrderByNombreAsc(
            Long idUsuario, EstadoServicio estado);

    @EntityGraph(attributePaths = { "artista", "tipoServicio", "etiquetas", "etiquetas.etiqueta" })
    List<Servicio> findByArtistaIdUsuarioInAndEstado(
            Collection<Long> idsArtista, EstadoServicio estado);

    List<Servicio> findByEstadoOrderByArchivadoEnDesc(EstadoServicio estado);

    boolean existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndEstadoNot(
            Long idUsuario, Long idTipoServicio, String nombre, EstadoServicio estado);

    boolean existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndIdServicioNotAndEstadoNot(
            Long idUsuario, Long idTipoServicio, String nombre, Long idServicio, EstadoServicio estado);
}
