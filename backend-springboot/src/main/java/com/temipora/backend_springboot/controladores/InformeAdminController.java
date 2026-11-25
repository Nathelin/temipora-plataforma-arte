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

import com.temipora.backend_springboot.modelo.InformeAdminDTO;
import com.temipora.backend_springboot.servicios.InformeAdminPdfService;
import com.temipora.backend_springboot.servicios.InformeAdminService;
import com.temipora.backend_springboot.servicios.UsuarioService;
import com.temipora.backend_springboot.servicios.informes.FormatoPdf;

@RestController
@RequestMapping("/api/admin/informes")
@PreAuthorize("hasRole('ADMIN')")
public class InformeAdminController {

    private final InformeAdminService informeAdminService;
    private final InformeAdminPdfService informeAdminPdfService;
    private final UsuarioService usuarioService;

    public InformeAdminController(
            InformeAdminService informeAdminService,
            InformeAdminPdfService informeAdminPdfService,
            UsuarioService usuarioService) {
        this.informeAdminService = informeAdminService;
        this.informeAdminPdfService = informeAdminPdfService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/ventas")
    public InformeAdminDTO ventas(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String agrupacion) {
        return informeAdminService.generar(
            desde, hasta, tipo, agrupacion);
    }

    @GetMapping("/ventas/pdf")
    public ResponseEntity<byte[]> ventasPdf(
            Authentication authentication,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String agrupacion) {
        InformeAdminDTO informe = informeAdminService.generar(
            desde, hasta, tipo, agrupacion);
        byte[] pdf = informeAdminPdfService.generar(
            informe, usuarioService.emisor(authentication.getName()));

        ContentDisposition disposition = ContentDisposition.attachment()
            .filename("informe-ventas-comisiones-"
                + FormatoPdf.fechaArchivo(informe.periodo().desde()) + "_al_"
                + FormatoPdf.fechaArchivo(informe.periodo().hasta()) + ".pdf",
                StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(pdf);
    }
}
