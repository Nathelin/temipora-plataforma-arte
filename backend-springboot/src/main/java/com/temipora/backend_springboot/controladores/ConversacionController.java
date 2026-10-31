package com.temipora.backend_springboot.controladores;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.BandejaMensajes;
import com.temipora.backend_springboot.modelo.ConfiguracionMensajeriaDTO;
import com.temipora.backend_springboot.modelo.ContadorMensajesDTO;
import com.temipora.backend_springboot.modelo.ConversacionDTO;
import com.temipora.backend_springboot.modelo.ConversacionResumenDTO;
import com.temipora.backend_springboot.modelo.EstadoConversacion;
import com.temipora.backend_springboot.modelo.EstadoRelacionMensajeDTO;
import com.temipora.backend_springboot.modelo.HistorialMensajesDTO;
import com.temipora.backend_springboot.modelo.MensajeDTO;
import com.temipora.backend_springboot.modelo.MensajeRequest;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.RechazoConversacionRequest;
import com.temipora.backend_springboot.modelo.SolicitudMensajeRequest;
import com.temipora.backend_springboot.servicios.ConfiguracionMensajeriaService;
import com.temipora.backend_springboot.servicios.ConversacionService;
import com.temipora.backend_springboot.servicios.MensajeService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/conversaciones")
@PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
public class ConversacionController {

    private final ConversacionService conversacionService;
    private final MensajeService mensajeService;
    private final ConfiguracionMensajeriaService configuracionService;
    private final UsuarioService usuarioService;

    public ConversacionController(
            ConversacionService conversacionService,
            MensajeService mensajeService,
            ConfiguracionMensajeriaService configuracionService,
            UsuarioService usuarioService) {
        this.conversacionService = conversacionService;
        this.mensajeService = mensajeService;
        this.configuracionService = configuracionService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<ConversacionDTO> crear(
            @Valid @RequestBody SolicitudMensajeRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(conversacionService.crear(idAutenticado(authentication), request));
    }

    @GetMapping
    public PaginaDTO<ConversacionResumenDTO> listar(
            @RequestParam(required = false) EstadoConversacion estado,
            @RequestParam(defaultValue = "false") boolean archivadas,
            @RequestParam(defaultValue = "TODAS") BandejaMensajes bandeja,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            Authentication authentication) {
        return conversacionService.listar(
            idAutenticado(authentication),
            estado,
            archivadas,
            bandeja,
            pagina,
            tamanio);
    }

    @GetMapping("/contador")
    public ContadorMensajesDTO contador(Authentication authentication) {
        return conversacionService.contador(idAutenticado(authentication));
    }

    @GetMapping("/con-usuario/{nombreUsuario}")
    public EstadoRelacionMensajeDTO estadoConUsuario(
            @PathVariable String nombreUsuario,
            Authentication authentication) {
        return conversacionService.estadoConUsuario(
            idAutenticado(authentication), nombreUsuario);
    }

    @GetMapping("/configuracion")
    public ConfiguracionMensajeriaDTO configuracion() {
        return configuracionService.obtenerEstado();
    }

    @GetMapping("/{idConversacion}")
    public ConversacionDTO obtener(
            @PathVariable Long idConversacion,
            Authentication authentication) {
        return conversacionService.obtener(
            idConversacion, idAutenticado(authentication));
    }

    @PostMapping("/{idConversacion}/aceptar")
    public ConversacionDTO aceptar(
            @PathVariable Long idConversacion,
            Authentication authentication) {
        return conversacionService.aceptar(
            idConversacion, idAutenticado(authentication));
    }

    @PostMapping("/{idConversacion}/rechazar")
    public ConversacionDTO rechazar(
            @PathVariable Long idConversacion,
            @Valid @RequestBody(required = false) RechazoConversacionRequest request,
            Authentication authentication) {
        return conversacionService.rechazar(
            idConversacion, idAutenticado(authentication), request);
    }

    @PostMapping("/{idConversacion}/cancelar")
    public ConversacionDTO cancelar(
            @PathVariable Long idConversacion,
            Authentication authentication) {
        return conversacionService.cancelar(
            idConversacion, idAutenticado(authentication));
    }

    @PostMapping("/{idConversacion}/archivar")
    public ConversacionDTO archivar(
            @PathVariable Long idConversacion,
            Authentication authentication) {
        return conversacionService.archivar(
            idConversacion, idAutenticado(authentication), true);
    }

    @PostMapping("/{idConversacion}/desarchivar")
    public ConversacionDTO desarchivar(
            @PathVariable Long idConversacion,
            Authentication authentication) {
        return conversacionService.archivar(
            idConversacion, idAutenticado(authentication), false);
    }

    @GetMapping("/{idConversacion}/mensajes")
    public HistorialMensajesDTO historial(
            @PathVariable Long idConversacion,
            @RequestParam(required = false) Long antesDe,
            @RequestParam(required = false) Integer tamanio,
            Authentication authentication) {
        return mensajeService.historial(
            idConversacion,
            idAutenticado(authentication),
            antesDe,
            tamanio);
    }

    @PostMapping("/{idConversacion}/mensajes")
    public ResponseEntity<MensajeDTO> enviar(
            @PathVariable Long idConversacion,
            @Valid @RequestBody MensajeRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mensajeService.enviar(
                idConversacion,
                idAutenticado(authentication),
                request));
    }

    @PostMapping("/{idConversacion}/leer")
    public ResponseEntity<Void> marcarLeido(
            @PathVariable Long idConversacion,
            Authentication authentication) {
        mensajeService.marcarLeido(
            idConversacion, idAutenticado(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
