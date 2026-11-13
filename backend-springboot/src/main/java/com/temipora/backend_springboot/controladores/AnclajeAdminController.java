package com.temipora.backend_springboot.controladores;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.AnclajeDTO;
import com.temipora.backend_springboot.modelo.AnclajeLedger;
import com.temipora.backend_springboot.servicios.AnclajeService;

/** Administración del anclaje del ledger en Bitcoin (OpenTimestamps). */
@RestController
@RequestMapping("/api/admin/anclaje")
@PreAuthorize("hasRole('ADMIN')")
public class AnclajeAdminController {

    private final AnclajeService anclajeService;

    public AnclajeAdminController(AnclajeService anclajeService) {
        this.anclajeService = anclajeService;
    }

    @GetMapping
    public List<AnclajeDTO> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return anclajeService.listar(pagina, tamanio).stream()
            .map(AnclajeService::aDTO)
            .toList();
    }

    /** Fuerza el sellado de los eventos nuevos y reintenta confirmar pendientes. */
    @PostMapping("/ejecutar")
    public ResponseEntity<?> ejecutar() {
        var anclaje = anclajeService.anclar();
        int confirmados = anclajeService.confirmarPendientes();
        if (anclaje.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "mensaje", "No hay eventos nuevos para sellar.",
                "confirmados", confirmados));
        }
        return ResponseEntity.ok(AnclajeService.aDTO(anclaje.get()));
    }

    /** Descarga el archivo .ots (verificable con la herramienta oficial). */
    @GetMapping("/{id}/prueba")
    public ResponseEntity<byte[]> prueba(@PathVariable Long id) {
        AnclajeLedger anclaje = anclajeService.porId(id).orElseThrow(
            () -> new java.util.NoSuchElementException("Anclaje no encontrado: " + id));
        byte[] prueba = anclaje.getPruebaConfirmada() != null
            ? anclaje.getPruebaConfirmada()
            : anclaje.getPruebaOts();
        if (prueba == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"anclaje-" + id + ".ots\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(prueba);
    }
}
