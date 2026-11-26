package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ApelacionSancion;
import com.temipora.backend_springboot.modelo.EstadoApelacion;

public interface ApelacionSancionRepository
        extends JpaRepository<ApelacionSancion, Long> {

    List<ApelacionSancion> findBySancionIdSancionOrderByCreadaEnDesc(Long idSancion);

    List<ApelacionSancion> findByUsuarioIdUsuarioOrderByCreadaEnDesc(Long idUsuario);

    List<ApelacionSancion> findByEstadoOrderByCreadaEnAsc(EstadoApelacion estado);

    Optional<ApelacionSancion> findFirstBySancionIdSancionAndEstado(
            Long idSancion, EstadoApelacion estado);

    Optional<ApelacionSancion> findFirstBySancionIdSancionOrderByCreadaEnDesc(
            Long idSancion);
}
