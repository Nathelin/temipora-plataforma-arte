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

import com.temipora.backend_springboot.modelo.TipoProductoDTO;
import com.temipora.backend_springboot.modelo.TipoProductoRequest;
import com.temipora.backend_springboot.servicios.TipoProductoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/tipos-producto")
@PreAuthorize("hasRole('ADMIN')")
public class TipoProductoController {

    private final TipoProductoService tipoProductoService;

    public TipoProductoController(TipoProductoService tipoProductoService) {
        this.tipoProductoService = tipoProductoService;
    }

    @GetMapping
    public List<TipoProductoDTO> listar() {
        return tipoProductoService.listar();
    }

    @PostMapping
    public ResponseEntity<TipoProductoDTO> crear(@Valid @RequestBody TipoProductoRequest request) {
        TipoProductoDTO dto = tipoProductoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public TipoProductoDTO modificar(
            @PathVariable Long id,
            @Valid @RequestBody TipoProductoRequest request) {
        return tipoProductoService.modificar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TipoProductoDTO> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(tipoProductoService.darDeBaja(id));
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<TipoProductoDTO> darDeAlta(@PathVariable Long id) {
        return ResponseEntity.ok(tipoProductoService.darDeAlta(id));
    }
}