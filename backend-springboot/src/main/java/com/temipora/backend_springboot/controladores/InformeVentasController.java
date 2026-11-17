package com.temipora.backend_springboot.controladores;

import java.nio.charset.StandardCharsets;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.InformeVentasDTO;
import com.temipora.backend_springboot.servicios.InformePdfService;
import com.temipora.backend_springboot.servicios.InformeVentasService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api/panel-artista/informes")
public class InformeVentasController {

    private final InformeVentasService informeVentasService;
    private final InformePdfService informePdfService;
    private final UsuarioService usuarioService;

    public InformeVentasController(
            InformeVentasService informeVentasService,
            InformePdfService informePdfService,
            UsuarioService usuarioService) {
        this.informeVentasService = informeVentasService;
        this.informePdfService = informePdfService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/ventas")
    @PreAuthorize("hasRole('ARTISTA')")
    public InformeVentasDTO ventas(
            Authentication authentication,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String agrupacion) {
        return informeVentasService.generar(
            idArtista(authentication), desde, hasta, tipo, agrupacion);
    }

    @GetMapping("/ventas/pdf")
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<byte[]> ventasPdf(
            Authentication authentication,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String agrupacion) {
        InformeVentasDTO informe = informeVentasService.generar(
            idArtista(authentication), desde, hasta, tipo, agrupacion);
        byte[] pdf = informePdfService.generar(
            informe, authentication.getName());

        ContentDisposition disposition = ContentDisposition.attachment()
            .filename("informe-ventas-" + informe.periodo().desde()
                + "_" + informe.periodo().hasta() + ".pdf",
                StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(pdf);
    }

    private Long idArtista(Authentication authentication) {
        return usuarioService
            .getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
