package com.temipora.backend_springboot.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.AccionSolicitud;
import com.temipora.backend_springboot.modelo.TipoAccionSolicitud;

public interface AccionSolicitudRepository extends JpaRepository<AccionSolicitud, Long> {

    Optional<AccionSolicitud> findFirstBySolicitudIdSolicitudAndTipoAccionOrderByFechaDesc(
            Long idSolicitud, TipoAccionSolicitud tipoAccion);

    Optional<AccionSolicitud> findFirstBySolicitudIdSolicitudOrderByFechaDescIdAccionSolicitudDesc(
            Long idSolicitud);
}
