package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.EstadoReporteResenia;
import com.temipora.backend_springboot.modelo.OcultarReseniaRequest;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.ReporteReseniaHistorialDTO;
import com.temipora.backend_springboot.modelo.ReseniaReportadaDTO;
import com.temipora.backend_springboot.servicios.ReseniaModeracionService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/resenias")
@PreAuthorize("hasRole('ADMIN')")
public class ReseniaModeracionAdminController {

    private final ReseniaModeracionService moderacionService;
    private final UsuarioService usuarioService;

    public ReseniaModeracionAdminController(
            ReseniaModeracionService moderacionService,
            UsuarioService usuarioService) {
        this.moderacionService = moderacionService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/reportadas")
    public PaginaDTO<ReseniaReportadaDTO> reportadas(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return moderacionService.listarReportadas(pagina, tamanio);
    }

    @GetMapping("/ocultas")
    public PaginaDTO<ReseniaReportadaDTO> ocultas(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return moderacionService.listarOcultas(pagina, tamanio);
    }

    @GetMapping("/historial")
    public PaginaDTO<ReporteReseniaHistorialDTO> historial(
            @RequestParam(required = false) EstadoReporteResenia estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return moderacionService.listarHistorial(estado, pagina, tamanio);
    }

    @PostMapping("/{idResenia}/ocultar")
    public ReseniaReportadaDTO ocultar(
            @PathVariable Long idResenia,
            @Valid @RequestBody(required = false) OcultarReseniaRequest request,
            Authentication authentication) {
        String motivo = request != null ? request.getMotivo() : null;
        return moderacionService.ocultar(
            idResenia, idAutenticado(authentication), motivo);
    }

    @PostMapping("/{idResenia}/descartar-reportes")
    public ReseniaReportadaDTO descartarReportes(
            @PathVariable Long idResenia,
            Authentication authentication) {
        return moderacionService.descartarReportes(
            idResenia, idAutenticado(authentication));
    }

    @PostMapping("/{idResenia}/restaurar")
    public ReseniaReportadaDTO restaurar(
            @PathVariable Long idResenia,
            Authentication authentication) {
        return moderacionService.restaurar(
            idResenia, idAutenticado(authentication));
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
