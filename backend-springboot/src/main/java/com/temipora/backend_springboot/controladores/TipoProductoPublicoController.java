package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.TipoProductoDTO;
import com.temipora.backend_springboot.servicios.TipoProductoService;

@RestController
@RequestMapping("/api")
public class TipoProductoPublicoController {

    private final TipoProductoService tipoProductoService;

    public TipoProductoPublicoController(TipoProductoService tipoProductoService) {
        this.tipoProductoService = tipoProductoService;
    }

    @GetMapping("/tipos-producto")
    public List<TipoProductoDTO> listar() {
        return tipoProductoService.listarActivos();
    }
}
