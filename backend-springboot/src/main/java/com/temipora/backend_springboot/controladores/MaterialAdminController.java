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

import com.temipora.backend_springboot.modelo.MaterialDTO;
import com.temipora.backend_springboot.modelo.MaterialRequest;
import com.temipora.backend_springboot.servicios.MaterialService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/materiales")
@PreAuthorize("hasRole('ADMIN')")
public class MaterialAdminController {

    private final MaterialService materialService;

    public MaterialAdminController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public List<MaterialDTO> listar() {
        return materialService.listar();
    }

    @PostMapping
    public ResponseEntity<MaterialDTO> crear(@Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.crear(request));
    }

    @PutMapping("/{id}")
    public MaterialDTO modificar(
            @PathVariable Long id,
            @Valid @RequestBody MaterialRequest request) {
        return materialService.modificar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MaterialDTO> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.darDeBaja(id));
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<MaterialDTO> darDeAlta(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.darDeAlta(id));
    }
}
