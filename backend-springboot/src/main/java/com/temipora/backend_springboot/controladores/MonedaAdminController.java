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

import com.temipora.backend_springboot.modelo.MonedaDTO;
import com.temipora.backend_springboot.modelo.MonedaRequest;
import com.temipora.backend_springboot.servicios.MonedaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/monedas")
@PreAuthorize("hasRole('ADMIN')")
public class MonedaAdminController {

    private final MonedaService monedaService;

    public MonedaAdminController(MonedaService monedaService) {
        this.monedaService = monedaService;
    }

    @GetMapping
    public List<MonedaDTO> listar() {
        return monedaService.listar();
    }

    @PostMapping
    public ResponseEntity<MonedaDTO> crear(@Valid @RequestBody MonedaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(monedaService.crear(request));
    }

    @PutMapping("/{id}")
    public MonedaDTO modificar(
            @PathVariable Long id,
            @Valid @RequestBody MonedaRequest request) {
        return monedaService.modificar(id, request);
    }

    @PostMapping("/{id}/predeterminada")
    public MonedaDTO marcarPredeterminada(@PathVariable Long id) {
        return monedaService.marcarPredeterminada(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MonedaDTO> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(monedaService.darDeBaja(id));
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<MonedaDTO> darDeAlta(@PathVariable Long id) {
        return ResponseEntity.ok(monedaService.darDeAlta(id));
    }
}
