package com.temipora.backend_springboot.controladores;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.AccesoEnlaceRequest;
import com.temipora.backend_springboot.servicios.AccesoEnlaceInvalidoException;
import com.temipora.backend_springboot.servicios.AccesoEnlaceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/acceso")
public class AccesoEnlaceController {

    private final AccesoEnlaceService accesoEnlaceService;

    public AccesoEnlaceController(AccesoEnlaceService accesoEnlaceService) {
        this.accesoEnlaceService = accesoEnlaceService;
    }

    @PostMapping
    public ResponseEntity<?> consumir(
            @Valid @RequestBody AccesoEnlaceRequest request) {
        try {
            return ResponseEntity.ok(
                accesoEnlaceService.consumir(request.getToken()));
        } catch (AccesoEnlaceInvalidoException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        }
    }
}
