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

import com.temipora.backend_springboot.modelo.HistorialClienteDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.HistorialClienteService;
import com.temipora.backend_springboot.servicios.InformeHistorialPdfService;
import com.temipora.backend_springboot.servicios.UsuarioService;
import com.temipora.backend_springboot.servicios.informes.FormatoPdf;

@RestController
@RequestMapping("/api/mi-historial")
@PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
public class HistorialClienteController {

    private final HistorialClienteService historialService;
    private final InformeHistorialPdfService pdfService;
    private final UsuarioService usuarioService;

    public HistorialClienteController(
            HistorialClienteService historialService,
            InformeHistorialPdfService pdfService,
            UsuarioService usuarioService) {
        this.historialService = historialService;
        this.pdfService = pdfService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public HistorialClienteDTO listar(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(defaultValue = "TODOS") String tipo,
            @RequestParam(defaultValue = "FINALIZADO") String estado,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamanio,
            Authentication authentication) {
        Usuario usuario = usuario(authentication);
        return historialService.listar(
            usuario.getIdUsuario(), desde, hasta, tipo, estado, q,
            pagina, tamanio);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(defaultValue = "TODOS") String tipo,
            @RequestParam(defaultValue = "FINALIZADO") String estado,
            @RequestParam(required = false) String q,
            Authentication authentication) {
        Usuario usuario = usuario(authentication);
        HistorialClienteDTO informe = historialService.listar(
            usuario.getIdUsuario(), desde, hasta, tipo, estado, q, 0,
            HistorialClienteService.FILAS_PDF);
        byte[] pdf = pdfService.generar(informe, nombre(usuario));

        ContentDisposition disposition = ContentDisposition.attachment()
            .filename("mi-historial-" + periodoArchivo(informe) + ".pdf",
                StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(pdf);
    }

    private Usuario usuario(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName());
    }

    private static String nombre(Usuario usuario) {
        if (usuario.getNombreDisplay() != null
                && !usuario.getNombreDisplay().isBlank()) {
            return usuario.getNombreDisplay();
        }
        return usuario.getNombreUsuario();
    }

    private static String periodoArchivo(HistorialClienteDTO informe) {
        if (informe.desde() != null && informe.hasta() != null) {
            return FormatoPdf.fechaArchivo(informe.desde()) + "_al_"
                + FormatoPdf.fechaArchivo(informe.hasta());
        }
        if (informe.desde() != null) {
            return "desde_" + FormatoPdf.fechaArchivo(informe.desde());
        }
        if (informe.hasta() != null) {
            return "hasta_" + FormatoPdf.fechaArchivo(informe.hasta());
        }
        return "completo";
    }
}
