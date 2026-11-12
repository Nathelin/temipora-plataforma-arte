package com.temipora.backend_springboot.controladores;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.ProductoCatalogoDTO;
import com.temipora.backend_springboot.modelo.ReventaDetalleDTO;
import com.temipora.backend_springboot.servicios.ProductoCatalogoService;
import com.temipora.backend_springboot.servicios.ReventaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api/reventas")
public class ReventaPublicoController {

    private final ProductoCatalogoService catalogoService;
    private final ReventaService reventaService;
    private final UsuarioService usuarioService;

    public ReventaPublicoController(
            ProductoCatalogoService catalogoService,
            ReventaService reventaService,
            UsuarioService usuarioService) {
        this.catalogoService = catalogoService;
        this.reventaService = reventaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public PaginaDTO<ProductoCatalogoDTO> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "24") int tamanio,
            Authentication authentication) {
        return catalogoService.listarReventas(
            idVisor(authentication), pagina, tamanio);
    }

    @GetMapping("/{idPublicacion}")
    public ReventaDetalleDTO detalle(
            @PathVariable Long idPublicacion,
            Authentication authentication) {
        return reventaService.detallePublico(
            idPublicacion, idVisor(authentication));
    }

    private Long idVisor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        try {
            return usuarioService.getByNombreUsuario(authentication.getName())
                .getIdUsuario();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
