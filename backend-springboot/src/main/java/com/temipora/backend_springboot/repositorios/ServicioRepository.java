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

import com.temipora.backend_springboot.modelo.Servicio;

import jakarta.persistence.LockModeType;

public interface ServicioRepository extends JpaRepository<Servicio, Long>, JpaSpecificationExecutor<Servicio> {

    Optional<Servicio> findByIdServicio(Long idServicio);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Servicio s WHERE s.idServicio = :idServicio")
    Optional<Servicio> findByIdServicioForUpdate(@Param("idServicio") Long idServicio);

    List<Servicio> findByArtistaIdUsuarioOrderByNombreAsc(Long idUsuario);

    @EntityGraph(attributePaths = { "artista", "tipoServicio", "etiquetas", "etiquetas.etiqueta" })
    List<Servicio> findByArtistaIdUsuarioInAndEstadoTrue(Collection<Long> idsArtista);

    boolean existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCase(
            Long idUsuario, Long idTipoServicio, String nombre);

    boolean existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndIdServicioNot(
            Long idUsuario, Long idTipoServicio, String nombre, Long idServicio);
}
