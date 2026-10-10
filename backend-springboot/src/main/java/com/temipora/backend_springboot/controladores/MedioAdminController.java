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

import com.temipora.backend_springboot.modelo.MedioDTO;
import com.temipora.backend_springboot.modelo.MedioRequest;
import com.temipora.backend_springboot.servicios.MedioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/medios")
@PreAuthorize("hasRole('ADMIN')")
public class MedioAdminController {

    private final MedioService medioService;

    public MedioAdminController(MedioService medioService) {
        this.medioService = medioService;
    }

    @GetMapping
    public List<MedioDTO> listar() {
        return medioService.listar();
    }

    @PostMapping
    public ResponseEntity<MedioDTO> crear(@Valid @RequestBody MedioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medioService.crear(request));
    }

    @PutMapping("/{id}")
    public MedioDTO modificar(
            @PathVariable Long id,
            @Valid @RequestBody MedioRequest request) {
        return medioService.modificar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MedioDTO> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(medioService.darDeBaja(id));
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<MedioDTO> darDeAlta(@PathVariable Long id) {
        return ResponseEntity.ok(medioService.darDeAlta(id));
    }
}
