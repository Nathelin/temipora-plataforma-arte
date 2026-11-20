package com.temipora.backend_springboot.controladores;

import java.time.Duration;
import java.util.NoSuchElementException;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.servicios.LocalStorageService;

/**
 * Sirve las copias de visualización de {@code /uploads} en el ancho pedido.
 * El original ({@code <uuid>.webp}) se sigue sirviendo tal cual desde el
 * handler estático: acá solo se leen sus píxeles para generar una copia nueva.
 */
@RestController
public class ImagenVarianteController {

    private static final CacheControl CACHE_INMUTABLE = CacheControl
        .maxAge(Duration.ofDays(365))
        .cachePublic()
        .immutable();

    private final LocalStorageService storageService;

    public ImagenVarianteController(LocalStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/uploads/{ancho}/{nombre}")
    public ResponseEntity<Resource> variante(
            @PathVariable String ancho,
            @PathVariable String nombre) {
        int valor;
        try {
            valor = Integer.parseInt(ancho);
        } catch (NumberFormatException e) {
            throw new NoSuchElementException("Imagen no encontrada.");
        }

        Resource recurso = storageService.abrirVariante(nombre, valor);
        return ResponseEntity.ok()
            .cacheControl(CACHE_INMUTABLE)
            .contentType(MediaType.parseMediaType("image/webp"))
            .body(recurso);
    }
}
