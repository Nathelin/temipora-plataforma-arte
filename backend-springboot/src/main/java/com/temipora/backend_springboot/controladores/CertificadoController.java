package com.temipora.backend_springboot.controladores;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.CertificadoDTO;
import com.temipora.backend_springboot.servicios.CertificadoService;

/** Certificado público de autenticidad de una pieza única (acceso por código). */
@RestController
@RequestMapping("/api/certificados")
public class CertificadoController {

    private final CertificadoService certificadoService;

    public CertificadoController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    @GetMapping("/{codigo}")
    public CertificadoDTO obtener(@PathVariable String codigo) {
        return certificadoService.obtener(codigo);
    }
}
