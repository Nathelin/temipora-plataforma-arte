package com.temipora.backend_springboot.controladores;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.AccesoOfertaRequest;
import com.temipora.backend_springboot.servicios.AccesoOfertaInvalidoException;
import com.temipora.backend_springboot.servicios.AccesoOfertaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/acceso-oferta")
public class AccesoOfertaController {

    private final AccesoOfertaService accesoOfertaService;

    public AccesoOfertaController(AccesoOfertaService accesoOfertaService) {
        this.accesoOfertaService = accesoOfertaService;
    }

    @PostMapping("/verificar")
    public ResponseEntity<?> verificar(
            @Valid @RequestBody AccesoOfertaRequest request) {
        try {
            return ResponseEntity.ok(accesoOfertaService.verificar(
                request.getTokenCampana(), request.getTokenAcceso()));
        } catch (AccesoOfertaInvalidoException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> consumir(
            @Valid @RequestBody AccesoOfertaRequest request) {
        try {
            return ResponseEntity.ok(accesoOfertaService.consumir(
                request.getTokenCampana(), request.getTokenAcceso()));
        } catch (AccesoOfertaInvalidoException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        }
    }
}
