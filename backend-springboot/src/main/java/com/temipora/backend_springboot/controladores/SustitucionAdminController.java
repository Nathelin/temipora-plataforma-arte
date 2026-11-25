package com.temipora.backend_springboot.controladores;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ConfiguracionSustitucionDTO;
import com.temipora.backend_springboot.modelo.InformeSustitucionesHistorialDTO;
import com.temipora.backend_springboot.modelo.SustitucionDetalleHistorialDTO;
import com.temipora.backend_springboot.modelo.SustitucionResumenDTO;
import com.temipora.backend_springboot.servicios.CierreEmailService;
import com.temipora.backend_springboot.servicios.ConfiguracionSustitucionService;
import com.temipora.backend_springboot.servicios.InformeSustitucionesPdfService;
import com.temipora.backend_springboot.servicios.SustitucionHistorialService;
import com.temipora.backend_springboot.servicios.SustitucionService;
import com.temipora.backend_springboot.servicios.informes.FormatoPdf;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class SustitucionAdminController {

    private final ConfiguracionSustitucionService configuracionService;
    private final SustitucionService sustitucionService;
    private final CierreEmailService cierreEmailService;
    private final SustitucionHistorialService historialService;
    private final InformeSustitucionesPdfService pdfService;

    public SustitucionAdminController(
            ConfiguracionSustitucionService configuracionService,
            SustitucionService sustitucionService,
            CierreEmailService cierreEmailService,
            SustitucionHistorialService historialService,
            InformeSustitucionesPdfService pdfService) {
        this.configuracionService = configuracionService;
        this.sustitucionService = sustitucionService;
        this.cierreEmailService = cierreEmailService;
        this.historialService = historialService;
        this.pdfService = pdfService;
    }

    @GetMapping("/configuracion-sustitucion")
    public ConfiguracionSustitucionDTO obtener() {
        return configuracionService.obtenerDto();
    }

    @PutMapping("/configuracion-sustitucion")
    public ConfiguracionSustitucionDTO actualizar(
            @Valid @RequestBody ConfiguracionSustitucionDTO request) {
        return configuracionService.actualizar(request);
    }

    @GetMapping("/sustituciones/resumen")
    public SustitucionResumenDTO resumen() {
        SustitucionResumenDTO resumen = sustitucionService.resumen();
        resumen.setEmailsCierrePendientes(cierreEmailService.pendientes());
        return resumen;
    }

    @PostMapping("/cierres-email/procesar")
    public Map<String, Object> procesarCierres() {
        int enviados = cierreEmailService.procesarPendientes(true);
        return Map.of(
            "enviados", enviados,
            "pendientes", cierreEmailService.pendientes());
    }

    @GetMapping("/sustituciones/historial")
    public InformeSustitucionesHistorialDTO historial(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String disparador,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return historialService.generar(
            desde, hasta, q, estado, motivo, disparador, pagina, tamanio);
    }

    @GetMapping("/sustituciones/historial/pdf")
    public ResponseEntity<byte[]> historialPdf(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String disparador) {
        InformeSustitucionesHistorialDTO informe = historialService.generar(
            desde, hasta, q, estado, motivo, disparador, 0,
            SustitucionHistorialService.FILAS_PDF);
        byte[] pdf = pdfService.generar(informe);

        ContentDisposition disposition = ContentDisposition.attachment()
            .filename("historial-sustituciones-"
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

    @GetMapping("/sustituciones/historial/{idSustitucion}")
    public SustitucionDetalleHistorialDTO detalle(
            @PathVariable Long idSustitucion) {
        return historialService.detalle(idSustitucion);
    }
}
