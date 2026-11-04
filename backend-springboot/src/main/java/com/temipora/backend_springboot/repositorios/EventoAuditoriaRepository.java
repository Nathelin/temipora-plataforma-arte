package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EventoAuditoria;

public interface EventoAuditoriaRepository
        extends JpaRepository<EventoAuditoria, Long>, JpaSpecificationExecutor<EventoAuditoria> {

    @Query("SELECT DISTINCT e.entidad FROM EventoAuditoria e WHERE e.entidad IS NOT NULL ORDER BY e.entidad")
    List<String> entidadesRegistradas();

    @Query("SELECT DISTINCT e.accion FROM EventoAuditoria e ORDER BY e.accion")
    List<String> accionesRegistradas();

    long countByOcurridoEnBetween(
        LocalDateTime desde, LocalDateTime hasta);

    long countByAccionInAndOcurridoEnBetween(
        Collection<String> acciones, LocalDateTime desde, LocalDateTime hasta);

    long countByAccionAndOcurridoEnBetween(
        String accion, LocalDateTime desde, LocalDateTime hasta);

    long countByRequestId(String requestId);

    @Query("""
        SELECT e.requestId, COUNT(e)
        FROM EventoAuditoria e
        WHERE e.requestId IN :requestIds
        GROUP BY e.requestId
        """)
    List<Object[]> contarPorRequestIds(
        @Param("requestIds") Collection<String> requestIds);

    @Query("""
        SELECT e.actorNombre, COUNT(e)
        FROM EventoAuditoria e
        WHERE e.accion = :accion
          AND e.ocurridoEn >= :desde
          AND e.actorNombre IS NOT NULL
        GROUP BY e.actorNombre
        HAVING COUNT(e) >= :minimo
        ORDER BY COUNT(e) DESC
        """)
    List<Object[]> contarPorActor(
        @Param("accion") String accion,
        @Param("desde") LocalDateTime desde,
        @Param("minimo") int minimo);

    @Query("""
        SELECT e.actorNombre, COUNT(e)
        FROM EventoAuditoria e
        WHERE e.accion IN :acciones
          AND e.ocurridoEn >= :desde
          AND e.actorNombre IS NOT NULL
        GROUP BY e.actorNombre
        HAVING COUNT(e) >= :minimo
        ORDER BY COUNT(e) DESC
        """)
    List<Object[]> contarPorActorEnAcciones(
        @Param("acciones") Collection<String> acciones,
        @Param("desde") LocalDateTime desde,
        @Param("minimo") int minimo);
}
