package com.temipora.backend_springboot.controladores;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.GaleriaPedidosDTO;
import com.temipora.backend_springboot.servicios.GaleriaPublicaService;
import com.temipora.backend_springboot.servicios.ImagenGaleriaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api/galeria")
public class GaleriaPublicaController {

    private final GaleriaPublicaService galeriaService;
    private final ImagenGaleriaService imagenGaleriaService;
    private final UsuarioService usuarioService;

    public GaleriaPublicaController(
            GaleriaPublicaService galeriaService,
            ImagenGaleriaService imagenGaleriaService,
            UsuarioService usuarioService) {
        this.galeriaService = galeriaService;
        this.imagenGaleriaService = imagenGaleriaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/perfil/{nombreUsuario}")
    public GaleriaPedidosDTO listar(
            @PathVariable String nombreUsuario,
            @RequestParam(required = false) Long idServicio,
            @RequestParam(required = false) Long idArtista,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamanio,
            Authentication authentication) {
        return galeriaService.listar(
            nombreUsuario,
            idAutenticado(authentication),
            idServicio,
            idArtista,
            pagina,
            tamanio);
    }

    @GetMapping("/archivos/{idArchivo}/preview")
    public ResponseEntity<Resource> preview(
            @PathVariable Long idArchivo,
            Authentication authentication) {
        EntregaArchivo archivo = galeriaService.autorizarPreview(
            idArchivo, idAutenticado(authentication));

        ContentDisposition disposition = ContentDisposition.inline()
            .filename(
                "galeria-" + idArchivo + ".webp",
                StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("image/webp"))
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(imagenGaleriaService.abrirLimpia(archivo));
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
