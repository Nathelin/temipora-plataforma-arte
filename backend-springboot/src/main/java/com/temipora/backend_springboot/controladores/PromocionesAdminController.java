package com.temipora.backend_springboot.controladores;

import java.nio.charset.StandardCharsets;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.InformeDescuentosDTO;
import com.temipora.backend_springboot.servicios.InformeDescuentosPdfService;
import com.temipora.backend_springboot.servicios.PromocionDescuentoService;
import com.temipora.backend_springboot.servicios.informes.FormatoPdf;

@RestController
@RequestMapping("/api/admin/promociones")
@PreAuthorize("hasRole('ADMIN')")
public class PromocionesAdminController {

    private final PromocionDescuentoService descuentoService;
    private final InformeDescuentosPdfService pdfService;

    public PromocionesAdminController(
            PromocionDescuentoService descuentoService,
            InformeDescuentosPdfService pdfService) {
        this.descuentoService = descuentoService;
        this.pdfService = pdfService;
    }

    @GetMapping("/descuentos")
    public InformeDescuentosDTO descuentos(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean comisionCero,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return descuentoService.generar(
            desde, hasta, q, comisionCero, pagina, tamanio);
    }

    @GetMapping("/descuentos/pdf")
    public ResponseEntity<byte[]> descuentosPdf(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean comisionCero) {
        InformeDescuentosDTO informe = descuentoService.generar(
            desde, hasta, q, comisionCero, 0,
            PromocionDescuentoService.FILAS_PDF);
        byte[] pdf = pdfService.generar(informe);

        ContentDisposition disposition = ContentDisposition.attachment()
            .filename("descuentos-promociones-"
                + FormatoPdf.fechaArchivo(informe.desde()) + "_al_"
                + FormatoPdf.fechaArchivo(informe.hasta()) + ".pdf",
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
