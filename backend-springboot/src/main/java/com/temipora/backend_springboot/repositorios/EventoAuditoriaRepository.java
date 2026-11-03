package com.temipora.backend_springboot.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.temipora.backend_springboot.modelo.EventoAuditoria;

public interface EventoAuditoriaRepository
        extends JpaRepository<EventoAuditoria, Long>, JpaSpecificationExecutor<EventoAuditoria> {

    @Query("SELECT DISTINCT e.entidad FROM EventoAuditoria e WHERE e.entidad IS NOT NULL ORDER BY e.entidad")
    List<String> entidadesRegistradas();

    @Query("SELECT DISTINCT e.accion FROM EventoAuditoria e ORDER BY e.accion")
    List<String> accionesRegistradas();
}
