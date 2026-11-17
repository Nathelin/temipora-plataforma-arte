package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ConfiguracionBajaCuentaDTO;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.servicios.BajaCuentaService;
import com.temipora.backend_springboot.servicios.ConfiguracionBajaCuentaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class BajaCuentaAdminController {

    private final ConfiguracionBajaCuentaService configuracionService;
    private final BajaCuentaService bajaCuentaService;

    public BajaCuentaAdminController(
            ConfiguracionBajaCuentaService configuracionService,
            BajaCuentaService bajaCuentaService) {
        this.configuracionService = configuracionService;
        this.bajaCuentaService = bajaCuentaService;
    }

    @GetMapping("/configuracion-baja-cuenta")
    public ConfiguracionBajaCuentaDTO obtener() {
        return configuracionService.obtenerDto();
    }

    @PutMapping("/configuracion-baja-cuenta")
    public ConfiguracionBajaCuentaDTO actualizar(
            @Valid @RequestBody ConfiguracionBajaCuentaDTO request) {
        return configuracionService.actualizar(request);
    }

    @PostMapping("/usuarios/{idUsuario}/reactivar")
    public UsuarioDTO reactivar(@PathVariable Long idUsuario) {
        return bajaCuentaService.reactivarComoAdmin(idUsuario);
    }
}
