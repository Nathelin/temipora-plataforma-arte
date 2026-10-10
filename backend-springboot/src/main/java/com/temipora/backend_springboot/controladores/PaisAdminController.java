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

import com.temipora.backend_springboot.modelo.PaisDTO;
import com.temipora.backend_springboot.modelo.PaisRequest;
import com.temipora.backend_springboot.servicios.PaisService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/paises")
@PreAuthorize("hasRole('ADMIN')")
public class PaisAdminController {

    private final PaisService paisService;

    public PaisAdminController(PaisService paisService) {
        this.paisService = paisService;
    }

    @GetMapping
    public List<PaisDTO> listar() {
        return paisService.listar();
    }

    @PostMapping
    public ResponseEntity<PaisDTO> crear(@Valid @RequestBody PaisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paisService.crear(request));
    }

    @PutMapping("/{id}")
    public PaisDTO modificar(
            @PathVariable Long id,
            @Valid @RequestBody PaisRequest request) {
        return paisService.modificar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<PaisDTO> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(paisService.darDeBaja(id));
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<PaisDTO> darDeAlta(@PathVariable Long id) {
        return ResponseEntity.ok(paisService.darDeAlta(id));
    }
}
