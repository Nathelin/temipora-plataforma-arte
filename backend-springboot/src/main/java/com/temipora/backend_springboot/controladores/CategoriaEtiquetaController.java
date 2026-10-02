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

import com.temipora.backend_springboot.modelo.CategoriaEtiquetaDTO;
import com.temipora.backend_springboot.modelo.CategoriaEtiquetaRequest;
import com.temipora.backend_springboot.servicios.CategoriaEtiquetaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/categorias-etiqueta")
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaEtiquetaController {

    private final CategoriaEtiquetaService categoriaEtiquetaService;

    public CategoriaEtiquetaController(CategoriaEtiquetaService categoriaEtiquetaService) {
        this.categoriaEtiquetaService = categoriaEtiquetaService;
    }

    @GetMapping
    public List<CategoriaEtiquetaDTO> listar() {
        return categoriaEtiquetaService.listar();
    }

    @PostMapping
    public ResponseEntity<CategoriaEtiquetaDTO> crear(@Valid @RequestBody CategoriaEtiquetaRequest request) {
        CategoriaEtiquetaDTO dto = categoriaEtiquetaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public CategoriaEtiquetaDTO modificar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaEtiquetaRequest request) {
        return categoriaEtiquetaService.modificar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CategoriaEtiquetaDTO> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaEtiquetaService.darDeBaja(id));
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<CategoriaEtiquetaDTO> darDeAlta(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaEtiquetaService.darDeAlta(id));
    }
}
