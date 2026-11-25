package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.servicios.SancionPruebaService;

/**
 * Laboratorio de pruebas para el ciclo de sanciones por entrega. Permite
 * ejecutar el ciclo real, preparar Nivel 1, forzar Nivel 2, simular la
 * aprobación del cliente y revertir todo sin esperar a los jobs.
 */
@RestController
@RequestMapping("/api/admin/sanciones/pruebas")
@PreAuthorize("hasRole('ADMIN')")
public class SancionPruebaAdminController {

    private static final String USUARIO_POR_DEFECTO = "ArtistaMoroso";

    private final SancionPruebaService service;

    public SancionPruebaAdminController(SancionPruebaService service) {
        this.service = service;
    }

    @GetMapping("/estado")
    public SancionPruebaService.EstadoPrueba estado(
            @RequestParam(defaultValue = USUARIO_POR_DEFECTO)
            String usuario) {
        return service.estado(usuario);
    }

    @PostMapping("/procesar")
    public SancionPruebaService.ResultadoPrueba procesar(
            @RequestParam(defaultValue = USUARIO_POR_DEFECTO)
            String usuario) {
        return service.procesar(usuario);
    }

    @PostMapping("/nivel1")
    public SancionPruebaService.ResultadoPrueba nivel1(
            @RequestParam(defaultValue = USUARIO_POR_DEFECTO)
            String usuario) {
        return service.prepararNivel1(usuario);
    }

    @PostMapping("/nivel2")
    public SancionPruebaService.ResultadoPrueba nivel2(
            @RequestParam(defaultValue = USUARIO_POR_DEFECTO)
            String usuario) {
        return service.forzarNivel2(usuario);
    }

    @PostMapping("/curar")
    public SancionPruebaService.ResultadoPrueba curar(
            @RequestParam(defaultValue = USUARIO_POR_DEFECTO)
            String usuario) {
        return service.simularAprobacion(usuario);
    }

    @PostMapping("/revertir")
    public SancionPruebaService.ResultadoPrueba revertir(
            @RequestParam(defaultValue = USUARIO_POR_DEFECTO)
            String usuario) {
        return service.revertirTodo(usuario);
    }
}
