package com.temipora.backend_springboot.controladores;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ArtistaFavoritoDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.ServicioFavoritoDTO;
import com.temipora.backend_springboot.servicios.FavoritoService;

@RestController
@RequestMapping("/api/favoritos")
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    // ------------------------------------------------------------------
    // Artistas
    // ------------------------------------------------------------------

    @GetMapping("/artistas")
    public PaginaDTO<ArtistaFavoritoDTO> listarArtistas(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamanio,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "recientes") String orden,
            Authentication authentication) {
        return favoritoService.listarArtistas(
            authentication.getName(), pagina, tamanio, q, orden);
    }

    @GetMapping("/artistas/{nombreUsuario}")
    public Map<String, Boolean> estadoArtista(
            @PathVariable String nombreUsuario,
            Authentication authentication) {
        return Map.of(
            "esFavorito",
            favoritoService.esFavoritoArtista(authentication.getName(), nombreUsuario));
    }

    @PostMapping("/artistas/{nombreUsuario}")
    public ArtistaFavoritoDTO agregarArtista(
            @PathVariable String nombreUsuario,
            Authentication authentication) {
        return favoritoService.agregarArtista(authentication.getName(), nombreUsuario);
    }

    @DeleteMapping("/artistas/{nombreUsuario}")
    public Map<String, Object> eliminarArtista(
            @PathVariable String nombreUsuario,
            Authentication authentication) {
        favoritoService.eliminarArtista(authentication.getName(), nombreUsuario);
        return Map.of(
            "esFavorito", false,
            "message", "El artista se eliminó de tus favoritos.");
    }

    // ------------------------------------------------------------------
    // Servicios
    // ------------------------------------------------------------------

    @GetMapping("/servicios")
    public PaginaDTO<ServicioFavoritoDTO> listarServicios(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamanio,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "recientes") String orden,
            @RequestParam(defaultValue = "todos") String estado,
            Authentication authentication) {
        return favoritoService.listarServicios(
            authentication.getName(), pagina, tamanio, q, orden, estado);
    }

    @GetMapping("/servicios/ids")
    public List<Long> listarIdsServicios(Authentication authentication) {
        return favoritoService.listarIdsServicios(authentication.getName());
    }

    @GetMapping("/servicios/{idServicio}")
    public Map<String, Boolean> estadoServicio(
            @PathVariable Long idServicio,
            Authentication authentication) {
        return Map.of(
            "esFavorito",
            favoritoService.esFavoritoServicio(authentication.getName(), idServicio));
    }

    @PostMapping("/servicios/{idServicio}")
    public ServicioFavoritoDTO agregarServicio(
            @PathVariable Long idServicio,
            Authentication authentication) {
        return favoritoService.agregarServicio(authentication.getName(), idServicio);
    }

    @DeleteMapping("/servicios/{idServicio}")
    public Map<String, Object> eliminarServicio(
            @PathVariable Long idServicio,
            Authentication authentication) {
        favoritoService.eliminarServicio(authentication.getName(), idServicio);
        return Map.of(
            "esFavorito", false,
            "message", "El servicio se eliminó de tus favoritos.");
    }

    // ------------------------------------------------------------------
    // Productos
    // ------------------------------------------------------------------

    @GetMapping("/productos/ids")
    public List<Long> listarIdsProductos(Authentication authentication) {
        return favoritoService.listarIdsProductos(authentication.getName());
    }

    @GetMapping("/productos/{idProducto}")
    public Map<String, Boolean> estadoProducto(
            @PathVariable Long idProducto,
            Authentication authentication) {
        return Map.of(
            "esFavorito",
            favoritoService.esFavoritoProducto(authentication.getName(), idProducto));
    }

    @PostMapping("/productos/{idProducto}")
    public Map<String, Object> agregarProducto(
            @PathVariable Long idProducto,
            Authentication authentication) {
        favoritoService.agregarProducto(authentication.getName(), idProducto);
        return Map.of("esFavorito", true);
    }

    @DeleteMapping("/productos/{idProducto}")
    public Map<String, Object> eliminarProducto(
            @PathVariable Long idProducto,
            Authentication authentication) {
        favoritoService.eliminarProducto(authentication.getName(), idProducto);
        return Map.of(
            "esFavorito", false,
            "message", "El producto se eliminó de tus favoritos.");
    }
}
