package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.MiEstadoCuentaDTO;
import com.temipora.backend_springboot.servicios.MoraClienteService;
import com.temipora.backend_springboot.servicios.UsuarioService;

/**
 * Estado consolidado de la cuenta del usuario logueado: sanción vigente y
 * deudas por entregas finales impagas.
 */
@RestController
@RequestMapping("/api/mi-estado-cuenta")
public class MiEstadoCuentaController {

    private final MoraClienteService moraClienteService;
    private final UsuarioService usuarioService;

    public MiEstadoCuentaController(
            MoraClienteService moraClienteService,
            UsuarioService usuarioService) {
        this.moraClienteService = moraClienteService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ARTISTA', 'CLIENTE')")
    public MiEstadoCuentaDTO obtener(Authentication authentication) {
        Long idUsuario = usuarioService
            .getByNombreUsuario(authentication.getName())
            .getIdUsuario();
        return moraClienteService.estadoCuenta(idUsuario);
    }
}
