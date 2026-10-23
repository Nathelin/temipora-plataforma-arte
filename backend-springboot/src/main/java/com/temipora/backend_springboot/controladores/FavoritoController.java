package com.temipora.backend_springboot.controladores;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ArtistaFavoritoDTO;
import com.temipora.backend_springboot.servicios.FavoritoService;

@RestController
@RequestMapping("/api/favoritos")
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    @GetMapping
    public List<ArtistaFavoritoDTO> listar(Authentication authentication) {
        return favoritoService.listar(authentication.getName());
    }

    @GetMapping("/{nombreUsuario}")
    public Map<String, Boolean> estado(
            @PathVariable String nombreUsuario,
            Authentication authentication) {
        return Map.of(
            "esFavorito",
            favoritoService.esFavorito(authentication.getName(), nombreUsuario));
    }

    @PostMapping("/{nombreUsuario}")
    public ArtistaFavoritoDTO agregar(
            @PathVariable String nombreUsuario,
            Authentication authentication) {
        return favoritoService.agregar(authentication.getName(), nombreUsuario);
    }

    @DeleteMapping("/{nombreUsuario}")
    public Map<String, Object> eliminar(
            @PathVariable String nombreUsuario,
            Authentication authentication) {
        favoritoService.eliminar(authentication.getName(), nombreUsuario);
        return Map.of(
            "esFavorito", false,
            "message", "El artista se eliminó de tus favoritos.");
    }
}
