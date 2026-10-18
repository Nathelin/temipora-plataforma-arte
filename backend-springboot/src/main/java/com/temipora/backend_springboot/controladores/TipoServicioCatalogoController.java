package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.TipoServicioDTO;
import com.temipora.backend_springboot.servicios.TipoServicioService;

@RestController
@RequestMapping("/api/tipos-servicio")
public class TipoServicioCatalogoController {

    private final TipoServicioService tipoServicioService;

    public TipoServicioCatalogoController(TipoServicioService tipoServicioService) {
        this.tipoServicioService = tipoServicioService;
    }

    @GetMapping
    public List<TipoServicioDTO> listar() {
        return tipoServicioService.listarActivos();
    }
}
