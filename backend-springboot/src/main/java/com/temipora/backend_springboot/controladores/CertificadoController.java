package com.temipora.backend_springboot.controladores;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    /** Descarga la prueba OpenTimestamps (.ots) del anclaje de la pieza. */
    @GetMapping("/{codigo}/prueba")
    public ResponseEntity<byte[]> prueba(@PathVariable String codigo) {
        return certificadoService.pruebaOts(codigo)
            .map(ots -> ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"certificado-" + codigo + ".ots\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(ots))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Descarga el documento sellado (la raíz de Merkle), necesario para verificar. */
    @GetMapping("/{codigo}/documento")
    public ResponseEntity<byte[]> documento(@PathVariable String codigo) {
        return certificadoService.documento(codigo)
            .map(doc -> ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"certificado-" + codigo + ".txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(doc))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

