package com.temipora.backend_springboot.controladores;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.EventoAuditoriaDTO;
import com.temipora.backend_springboot.modelo.FiltroAuditoria;
import com.temipora.backend_springboot.modelo.OpcionesAuditoriaDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.ResumenAuditoriaDTO;
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
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String entidadId,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean critica,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        FiltroAuditoria filtro = new FiltroAuditoria(
            actor,
            origen,
            entidad,
            entidadId,
            accion,
            requestId,
            texto,
            categoria,
            critica,
            inicio(desde),
            fin(hasta));
        return auditoriaService.listar(filtro, pagina, tamanio);
    }

    @GetMapping("/resumen")
    public ResumenAuditoriaDTO resumen(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        LocalDateTime fin = fin(hasta);
        LocalDateTime inicio = inicio(desde);
        if (fin == null) {
            fin = LocalDateTime.now();
        }
        if (inicio == null) {
            inicio = fin.minusHours(24);
        }
        return auditoriaService.resumen(inicio, fin);
    }

    @GetMapping("/{id}")
    public EventoAuditoriaDTO obtener(@PathVariable Long id) {
        return auditoriaService.obtener(id);
    }

    @GetMapping("/opciones")
    public OpcionesAuditoriaDTO opciones() {
        return auditoriaService.opciones();
    }

    private LocalDateTime inicio(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return parse(valor);
    }

    private LocalDateTime fin(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        LocalDateTime fecha = parse(valor);
        return valor.trim().length() <= 10
            ? fecha.toLocalDate().atTime(23, 59, 59)
            : fecha;
    }

    private LocalDateTime parse(String valor) {
        String texto = valor.trim();
        try {
            return texto.length() <= 10
                ? LocalDate.parse(texto).atStartOfDay()
                : LocalDateTime.parse(texto);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Fecha inválida: " + valor + ". Usá AAAA-MM-DD o AAAA-MM-DDTHH:mm:ss.", e);
        }
    }
}
