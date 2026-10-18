package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.temipora.backend_springboot.modelo.Servicio;

public interface ServicioRepository extends JpaRepository<Servicio, Long>, JpaSpecificationExecutor<Servicio> {

    Optional<Servicio> findByIdServicio(Long idServicio);

    List<Servicio> findByArtistaIdUsuarioOrderByNombreAsc(Long idUsuario);

    boolean existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCase(
            Long idUsuario, Long idTipoServicio, String nombre);

    boolean existsByArtistaIdUsuarioAndTipoServicioIdTipoServicioAndNombreIgnoreCaseAndIdServicioNot(
            Long idUsuario, Long idTipoServicio, String nombre, Long idServicio);
}
