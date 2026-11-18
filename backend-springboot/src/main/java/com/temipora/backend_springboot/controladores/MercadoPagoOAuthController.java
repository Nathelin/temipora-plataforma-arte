package com.temipora.backend_springboot.controladores;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.CuentaCobroDTO;
import com.temipora.backend_springboot.servicios.UsuarioService;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;

@RestController
@RequestMapping("/api/pagos/mercadopago/oauth")
public class MercadoPagoOAuthController {

    private static final Logger log =
        LoggerFactory.getLogger(MercadoPagoOAuthController.class);

    private final MercadoPagoOAuthService oauthService;
    private final UsuarioService usuarioService;
    private final String frontendUrl;

    public MercadoPagoOAuthController(
            MercadoPagoOAuthService oauthService,
            UsuarioService usuarioService,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.oauthService = oauthService;
        this.usuarioService = usuarioService;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/inicio")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public Map<String, String> inicio(Authentication authentication) {
        return Map.of("url", oauthService.urlAutorizacion(
            idAutenticado(authentication)));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state) {
        String destino;
        try {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException(
                    "Falta el código de autorización.");
            }
            oauthService.procesarCallback(code, state);
            destino = urlPagosYCobros("ok");
        } catch (RuntimeException e) {
            log.warn("No se pudo conectar la cuenta de MercadoPago: {}",
                e.getMessage());
            destino = urlPagosYCobros("error");
        }
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(destino))
            .build();
    }

    /**
     * El frontendUrl puede venir con barra final; si no se normaliza, el
     * navegador recibe "//configuracion/..." y vue-router lo rechaza.
     */
    private String urlPagosYCobros(String resultado) {
        String base = frontendUrl != null && !frontendUrl.isBlank()
            ? frontendUrl.replaceAll("/+$", "")
            : "http://localhost:5173";
        return base + "/configuracion/pagos-y-cobros?mp=" + resultado;
    }

    @GetMapping("/estado")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public CuentaCobroDTO estado(Authentication authentication) {
        return oauthService.estado(idAutenticado(authentication));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public ResponseEntity<Void> desconectar(Authentication authentication) {
        oauthService.desconectar(idAutenticado(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
