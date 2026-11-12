package com.temipora.backend_springboot.controladores;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PaginaReseniasDTO;
import com.temipora.backend_springboot.modelo.ResumenReseniasDTO;
import com.temipora.backend_springboot.servicios.ReseniaPublicaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api")
public class ReseniaPublicaController {

    private final ReseniaPublicaService reseniaPublicaService;
    private final UsuarioService usuarioService;

    public ReseniaPublicaController(
            ReseniaPublicaService reseniaPublicaService,
            UsuarioService usuarioService) {
        this.reseniaPublicaService = reseniaPublicaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/servicios/{idServicio}/resenias")
    public PaginaReseniasDTO listarServicio(
            @PathVariable Long idServicio,
            @RequestParam(required = false) Long idOpcion,
            @RequestParam(required = false) Integer calificacion,
            @RequestParam(defaultValue = "RECIENTES") String orden,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return reseniaPublicaService.listarServicio(
            idServicio, idOpcion, calificacion, orden, pagina, tamanio);
    }

    @GetMapping("/perfil/{nombreUsuario}/resenias")
    public PaginaReseniasDTO listarArtista(
            @PathVariable String nombreUsuario,
            @RequestParam(required = false) Long idServicio,
            @RequestParam(required = false) Integer calificacion,
            @RequestParam(defaultValue = "RECIENTES") String orden,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        Long idArtista = usuarioService.getByNombreUsuario(nombreUsuario)
            .getIdUsuario();
        return reseniaPublicaService.listarArtista(
            idArtista, idServicio, calificacion, orden, pagina, tamanio);
    }

    @GetMapping("/productos/{idProducto}/resenias")
    public PaginaReseniasDTO listarProducto(
            @PathVariable Long idProducto,
            @RequestParam(required = false) Long idOpcion,
            @RequestParam(required = false) Integer calificacion,
            @RequestParam(defaultValue = "RECIENTES") String orden,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return reseniaPublicaService.listarProducto(
            idProducto, idOpcion, calificacion, orden, pagina, tamanio);
    }

    @GetMapping("/perfil/{nombreUsuario}/resumen-resenias")
    public ResumenReseniasDTO resumenArtista(
            @PathVariable String nombreUsuario) {
        Long idArtista = usuarioService.getByNombreUsuario(nombreUsuario)
            .getIdUsuario();
        return reseniaPublicaService.resumenArtista(idArtista);
    }

}
