package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.ListaEspera;

public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Long> {

    Optional<ListaEspera> findBySolicitudIdSolicitud(Long idSolicitud);

    boolean existsBySolicitudIdSolicitud(Long idSolicitud);

    List<ListaEspera> findByServicioIdServicioAndEstadoOrderByPosicionAsc(
            Long idServicio, String estado);

    @Query("""
        SELECT COALESCE(MAX(l.posicion), 0)
        FROM ListaEspera l
        WHERE l.servicio.idServicio = :idServicio
          AND l.estado = 'ESPERANDO'
        """)
    Integer maxPosicionActiva(@Param("idServicio") Long idServicio);
}
