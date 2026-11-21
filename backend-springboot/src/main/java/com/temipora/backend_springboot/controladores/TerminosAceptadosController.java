package com.temipora.backend_springboot.controladores;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.TerminosAceptadosDTO;
import com.temipora.backend_springboot.servicios.TerminosAceptadosService;

/** Verificación pública de los términos aceptados en un pedido (por código). */
@RestController
@RequestMapping("/api/terminos-aceptados")
public class TerminosAceptadosController {

    private final TerminosAceptadosService terminosAceptadosService;

    public TerminosAceptadosController(
            TerminosAceptadosService terminosAceptadosService) {
        this.terminosAceptadosService = terminosAceptadosService;
    }

    @GetMapping("/{codigo}")
    public TerminosAceptadosDTO obtener(@PathVariable String codigo) {
        return terminosAceptadosService.obtener(codigo);
    }
}
