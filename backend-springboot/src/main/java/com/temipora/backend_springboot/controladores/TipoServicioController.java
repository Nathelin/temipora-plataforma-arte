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

import com.temipora.backend_springboot.modelo.TipoServicioDTO;
import com.temipora.backend_springboot.modelo.TipoServicioRequest;
import com.temipora.backend_springboot.servicios.TipoServicioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/tipos-servicio")
@PreAuthorize("hasRole('ADMIN')")
public class TipoServicioController {

    private final TipoServicioService tipoServicioService;

    public TipoServicioController(TipoServicioService tipoServicioService) {
        this.tipoServicioService = tipoServicioService;
    }

    @GetMapping
    public List<TipoServicioDTO> listar() {
        return tipoServicioService.listar();
    }

    @PostMapping
    public ResponseEntity<TipoServicioDTO> crear(@Valid @RequestBody TipoServicioRequest request) {
        TipoServicioDTO dto = tipoServicioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public TipoServicioDTO modificar(
            @PathVariable Long id,
            @Valid @RequestBody TipoServicioRequest request) {
        return tipoServicioService.modificar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TipoServicioDTO> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(tipoServicioService.darDeBaja(id));
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<TipoServicioDTO> darDeAlta(@PathVariable Long id) {
        return ResponseEntity.ok(tipoServicioService.darDeAlta(id));
    }
}