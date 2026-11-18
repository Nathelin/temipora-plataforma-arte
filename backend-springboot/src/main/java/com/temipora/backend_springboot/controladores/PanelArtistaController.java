package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.PanelArtistaResumenDTO;
import com.temipora.backend_springboot.modelo.VentaDTO;
import com.temipora.backend_springboot.modelo.VentaProductoDTO;
import com.temipora.backend_springboot.servicios.PanelArtistaService;
import com.temipora.backend_springboot.servicios.UsuarioService;
import com.temipora.backend_springboot.servicios.VentaProductoService;
import com.temipora.backend_springboot.servicios.VentasService;

@RestController
@RequestMapping("/api/panel-artista")
public class PanelArtistaController {

    private final PanelArtistaService panelArtistaService;
    private final VentasService ventasService;
    private final VentaProductoService ventaProductoService;
    private final UsuarioService usuarioService;

    public PanelArtistaController(
            PanelArtistaService panelArtistaService,
            VentasService ventasService,
            VentaProductoService ventaProductoService,
            UsuarioService usuarioService) {
        this.panelArtistaService = panelArtistaService;
        this.ventasService = ventasService;
        this.ventaProductoService = ventaProductoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ARTISTA')")
    public PanelArtistaResumenDTO resumen(Authentication authentication) {
        return panelArtistaService.resumen(idArtista(authentication));
    }

    @GetMapping("/ventas")
    @PreAuthorize("hasRole('ARTISTA')")
    public PaginaDTO<VentaDTO> ventas(
            Authentication authentication,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) Long idServicio,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return ventasService.listar(
            idArtista(authentication), estado, mes, idServicio, pagina, tamanio);
    }

    @GetMapping("/ventas/productos")
    @PreAuthorize("hasRole('ARTISTA')")
    public PaginaDTO<VentaProductoDTO> ventasProductos(
            Authentication authentication,
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) Long idProducto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return ventaProductoService.listar(
            idArtista(authentication), mes, idProducto, pagina, tamanio);
    }

    @PostMapping("/ventas/{idPago}/sincronizar")
    @PreAuthorize("hasRole('ARTISTA')")
    public VentaDTO sincronizar(
            Authentication authentication,
            @PathVariable Long idPago) {
        return ventasService.sincronizar(
            idArtista(authentication), idPago);
    }

    private Long idArtista(Authentication authentication) {
        return usuarioService
            .getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
