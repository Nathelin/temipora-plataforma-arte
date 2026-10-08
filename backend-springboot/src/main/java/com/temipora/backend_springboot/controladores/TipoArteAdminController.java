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
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.TipoArteDTO;
import com.temipora.backend_springboot.modelo.TipoArteRequest;
import com.temipora.backend_springboot.servicios.TipoArteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/tipos-arte")
@PreAuthorize("hasRole('ADMIN')")
public class TipoArteAdminController {

    private final TipoArteService tipoArteService;

    public TipoArteAdminController(TipoArteService tipoArteService) {
        this.tipoArteService = tipoArteService;
    }

    @GetMapping
    public List<TipoArteDTO> listar() {
        return tipoArteService.listar();
    }

    @PostMapping
    public ResponseEntity<TipoArteDTO> crear(@Valid @RequestBody TipoArteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoArteService.crear(request));
    }

    @PutMapping("/{id}")
    public TipoArteDTO modificar(
            @PathVariable Long id,
            @Valid @RequestBody TipoArteRequest request) {
        return tipoArteService.modificar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TipoArteDTO> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(tipoArteService.darDeBaja(id));
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<TipoArteDTO> darDeAlta(@PathVariable Long id) {
        return ResponseEntity.ok(tipoArteService.darDeAlta(id));
    }
}
