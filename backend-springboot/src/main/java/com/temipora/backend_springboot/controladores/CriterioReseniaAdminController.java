package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.AmbitoResenia;
import com.temipora.backend_springboot.modelo.CriterioReseniaDTO;
import com.temipora.backend_springboot.modelo.CriterioReseniaRequest;
import com.temipora.backend_springboot.modelo.EscalaCriterio;
import com.temipora.backend_springboot.modelo.OpcionCriterioRequest;
import com.temipora.backend_springboot.servicios.CriterioReseniaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/criterios-resenia")
@PreAuthorize("hasRole('ADMIN')")
public class CriterioReseniaAdminController {

    private final CriterioReseniaService criterioReseniaService;

    public CriterioReseniaAdminController(
            CriterioReseniaService criterioReseniaService) {
        this.criterioReseniaService = criterioReseniaService;
    }

    @GetMapping
    public List<CriterioReseniaDTO> listar(
            @RequestParam(required = false) AmbitoResenia ambito,
            @RequestParam(required = false) EscalaCriterio escala,
            @RequestParam(required = false) Boolean activo) {
        return criterioReseniaService.listar(ambito, escala, activo);
    }

    @PostMapping
    public ResponseEntity<CriterioReseniaDTO> crear(
            @Valid @RequestBody CriterioReseniaRequest request) {
        CriterioReseniaDTO dto = criterioReseniaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public CriterioReseniaDTO modificar(
            @PathVariable Long id,
            @Valid @RequestBody CriterioReseniaRequest request) {
        return criterioReseniaService.modificar(id, request);
    }

    @DeleteMapping("/{id}")
    public CriterioReseniaDTO darDeBaja(@PathVariable Long id) {
        return criterioReseniaService.darDeBaja(id);
    }

    @PostMapping("/{id}/activar")
    public CriterioReseniaDTO darDeAlta(@PathVariable Long id) {
        return criterioReseniaService.darDeAlta(id);
    }

    @PostMapping("/{id}/opciones")
    public ResponseEntity<CriterioReseniaDTO> crearOpcion(
            @PathVariable Long id,
            @Valid @RequestBody OpcionCriterioRequest request) {
        CriterioReseniaDTO dto = criterioReseniaService.crearOpcion(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}/opciones/{idOpcion}")
    public CriterioReseniaDTO modificarOpcion(
            @PathVariable Long id,
            @PathVariable Long idOpcion,
            @Valid @RequestBody OpcionCriterioRequest request) {
        return criterioReseniaService.modificarOpcion(id, idOpcion, request);
    }

    @DeleteMapping("/{id}/opciones/{idOpcion}")
    public CriterioReseniaDTO darDeBajaOpcion(
            @PathVariable Long id,
            @PathVariable Long idOpcion) {
        return criterioReseniaService.darDeBajaOpcion(id, idOpcion);
    }

    @PostMapping("/{id}/opciones/{idOpcion}/activar")
    public CriterioReseniaDTO darDeAltaOpcion(
            @PathVariable Long id,
            @PathVariable Long idOpcion) {
        return criterioReseniaService.darDeAltaOpcion(id, idOpcion);
    }
}
