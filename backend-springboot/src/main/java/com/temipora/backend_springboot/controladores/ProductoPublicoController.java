package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.FiltroProductoRequest;
import com.temipora.backend_springboot.modelo.OrdenProducto;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.ProductoCatalogoDTO;
import com.temipora.backend_springboot.servicios.ProductoCatalogoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

/**
 * Catálogo público de productos. Sin autenticación obligatoria: si hay sesión
 * se filtra por las preferencias del viewer (contenido sensible, moneda) y se
 * excluyen sus propios productos.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoPublicoController {

    private final ProductoCatalogoService productoCatalogoService;
    private final UsuarioService usuarioService;

    public ProductoPublicoController(
            ProductoCatalogoService productoCatalogoService,
            UsuarioService usuarioService) {
        this.productoCatalogoService = productoCatalogoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public PaginaDTO<ProductoCatalogoDTO> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String nombreArtista,
            @RequestParam(required = false) Long idTipoProducto,
            @RequestParam(required = false) Long idMedio,
            @RequestParam(required = false) Boolean sensible,
            @RequestParam(required = false) java.math.BigDecimal precioMin,
            @RequestParam(required = false) java.math.BigDecimal precioMax,
            @RequestParam(required = false) Boolean soloConStock,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "24") int tamanio,
            @RequestParam(defaultValue = "RELEVANCIA") OrdenProducto orden,
            Authentication authentication) {
        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setTitulo(titulo);
        filtro.setNombreArtista(nombreArtista);
        filtro.setIdTipoProducto(idTipoProducto);
        filtro.setIdMedio(idMedio);
        filtro.setSensible(sensible);
        filtro.setPrecioMin(precioMin);
        filtro.setPrecioMax(precioMax);
        filtro.setSoloConStock(soloConStock);
        return productoCatalogoService.listar(
            filtro, idVisor(authentication), pagina, tamanio, orden);
    }

    @GetMapping("/artista/{nombreUsuario}")
    public List<ProductoCatalogoDTO> listarDeArtista(
            @PathVariable String nombreUsuario,
            Authentication authentication) {
        return productoCatalogoService.listarDeArtista(
            nombreUsuario, idVisor(authentication));
    }

    @GetMapping("/{idProducto}")
    public ProductoCatalogoDTO obtener(
            @PathVariable Long idProducto,
            Authentication authentication) {
        return productoCatalogoService.obtener(
            idProducto, idVisor(authentication));
    }

    private Long idVisor(Authentication authentication) {
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
