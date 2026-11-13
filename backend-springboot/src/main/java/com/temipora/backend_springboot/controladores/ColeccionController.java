package com.temipora.backend_springboot.controladores;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ColeccionProductosDTO;
import com.temipora.backend_springboot.modelo.ColeccionVisibilidadDTO;
import com.temipora.backend_springboot.modelo.ColeccionVisibilidadRequest;
import com.temipora.backend_springboot.servicios.AccesoDenegadoException;
import com.temipora.backend_springboot.servicios.ColeccionService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

/**
 * Colección de productos comprados. Es un recurso separado de la galería de
 * pedidos y del catálogo de productos que ofrece un artista: la colección
 * muestra lo que el usuario adquirió, no lo que vende.
 */
@RestController
@RequestMapping("/api/coleccion")
public class ColeccionController {

    private final ColeccionService coleccionService;
    private final UsuarioService usuarioService;

    public ColeccionController(
            ColeccionService coleccionService,
            UsuarioService usuarioService) {
        this.coleccionService = coleccionService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/perfil/{nombreUsuario}")
    public ColeccionProductosDTO listar(
            @PathVariable String nombreUsuario,
            @RequestParam(required = false) Long idArtista,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamanio,
            Authentication authentication) {
        return coleccionService.listar(
            nombreUsuario,
            idAutenticado(authentication),
            idArtista,
            pagina,
            tamanio);
    }

    @PutMapping("/items/{idCompraItem}/visibilidad")
    @PreAuthorize("isAuthenticated()")
    public ColeccionVisibilidadDTO cambiarVisibilidad(
            @PathVariable Long idCompraItem,
            @RequestBody @Valid ColeccionVisibilidadRequest request,
            Authentication authentication) {
        Long idUsuario = idAutenticado(authentication);
        if (idUsuario == null) {
            throw new AccesoDenegadoException("Tenés que iniciar sesión.");
        }
        return coleccionService.alternarOculto(
            idCompraItem, idUsuario, request.getOcultar());
    }

    @PostMapping("/sugerencia/vista")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarSugerenciaVista(Authentication authentication) {
        Long idUsuario = idAutenticado(authentication);
        if (idUsuario == null) {
            throw new AccesoDenegadoException("Tenés que iniciar sesión.");
        }
        coleccionService.marcarSugerenciaVista(idUsuario);
    }

    private Long idAutenticado(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
