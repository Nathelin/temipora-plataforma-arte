package com.temipora.backend_springboot.controladores;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.PaginaReseniasDTO;
import com.temipora.backend_springboot.modelo.ResumenReseniasDTO;
import com.temipora.backend_springboot.servicios.ImagenGaleriaService;
import com.temipora.backend_springboot.servicios.ReseniaPublicaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api")
public class ReseniaPublicaController {

    private final ReseniaPublicaService reseniaPublicaService;
    private final UsuarioService usuarioService;
    private final ImagenGaleriaService imagenGaleriaService;

    public ReseniaPublicaController(
            ReseniaPublicaService reseniaPublicaService,
            UsuarioService usuarioService,
            ImagenGaleriaService imagenGaleriaService) {
        this.reseniaPublicaService = reseniaPublicaService;
        this.usuarioService = usuarioService;
        this.imagenGaleriaService = imagenGaleriaService;
    }

    @GetMapping("/servicios/{idServicio}/resenias")
    public PaginaReseniasDTO listarServicio(
            @PathVariable Long idServicio,
            @RequestParam(required = false) Integer calificacion,
            @RequestParam(defaultValue = "RECIENTES") String orden,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return reseniaPublicaService.listarServicio(
            idServicio, calificacion, orden, pagina, tamanio);
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

    @GetMapping("/perfil/{nombreUsuario}/resumen-resenias")
    public ResumenReseniasDTO resumenArtista(
            @PathVariable String nombreUsuario) {
        Long idArtista = usuarioService.getByNombreUsuario(nombreUsuario)
            .getIdUsuario();
        return reseniaPublicaService.resumenArtista(idArtista);
    }

    @GetMapping("/resenias/imagenes/{idArchivo}/preview")
    public ResponseEntity<Resource> previewImagen(
            @PathVariable Long idArchivo) {
        EntregaArchivo archivo = reseniaPublicaService.autorizarImagen(idArchivo);
        ContentDisposition disposition = ContentDisposition.inline()
            .filename("resenia-" + idArchivo + ".webp")
            .build();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("image/webp"))
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(imagenGaleriaService.abrirLimpia(archivo));
    }
}
