package com.temipora.backend_springboot.controladores;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.EventoAuditoriaDTO;
import com.temipora.backend_springboot.modelo.OpcionesAuditoriaDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.servicios.AuditoriaService;

@RestController
@RequestMapping("/api/admin/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaAdminController {

    private final AuditoriaService auditoriaService;

    public AuditoriaAdminController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public PaginaDTO<EventoAuditoriaDTO> listar(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return auditoriaService.listar(
            actor,
            entidad,
            accion,
            desde != null ? desde.atStartOfDay() : null,
            hasta != null ? hasta.atTime(23, 59, 59) : null,
            pagina,
            tamanio);
    }

    @GetMapping("/opciones")
    public OpcionesAuditoriaDTO opciones() {
        return auditoriaService.opciones();
    }
}
