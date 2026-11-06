package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.SustitucionOpcion;

public interface SustitucionOpcionRepository
        extends JpaRepository<SustitucionOpcion, Long> {

    List<SustitucionOpcion> findBySustitucionIdSustitucionOrderByOrdenAsc(
            Long idSustitucion);

    Optional<SustitucionOpcion> findBySustitucionIdSustitucionAndServicioIdServicio(
            Long idSustitucion, Long idServicio);
}
