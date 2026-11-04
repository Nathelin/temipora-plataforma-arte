package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.temipora.backend_springboot.modelo.CuentaCobroDTO;
import com.temipora.backend_springboot.modelo.CuentaPagoUsuario;
import com.temipora.backend_springboot.modelo.EstadoCuentaPago;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CuentaPagoUsuarioRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.ConflictoException;

@Service
public class MercadoPagoOAuthService {

    private static final Logger log =
        LoggerFactory.getLogger(MercadoPagoOAuthService.class);

    public static final String PROVEEDOR = "MERCADOPAGO";

    private final MercadoPagoClient cliente;
    private final MercadoPagoOAuthState state;
    private final CuentaPagoUsuarioRepository cuentas;
    private final UsuarioRepository usuarios;
    private final CifradorTokens cifrador;
    private final String clientId;
    private final String oauthUrl;
    private final String frontendUrl;
    private final boolean oauthTestToken;

    public MercadoPagoOAuthService(
            MercadoPagoClient cliente,
            MercadoPagoOAuthState state,
            CuentaPagoUsuarioRepository cuentas,
            UsuarioRepository usuarios,
            CifradorTokens cifrador,
            @Value("${pagos.mercadopago.client-id:}") String clientId,
            @Value("${pagos.mercadopago.oauth-url:https://auth.mercadopago.com.ar/authorization}") String oauthUrl,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            @Value("${pagos.mercadopago.oauth-test-token:${pagos.mercadopago.sandbox:false}}") boolean oauthTestToken) {
        this.cliente = cliente;
        this.state = state;
        this.cuentas = cuentas;
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.clientId = clientId;
        this.oauthUrl = oauthUrl;
        this.frontendUrl = frontendUrl;
        this.oauthTestToken = oauthTestToken;
    }

    public String urlAutorizacion(Long idUsuario) {
        if (clientId == null || clientId.isBlank()) {
            throw new MercadoPagoException(
                "Falta configurar TEMIPORA_MP_CLIENT_ID.");
        }
        String url = UriComponentsBuilder.fromUriString(oauthUrl)
            .queryParam("client_id", clientId)
            .queryParam("response_type", "code")
            .queryParam("platform_id", "mp")
            .queryParam("redirect_uri", redirectUri())
            .queryParam("state", state.generar(idUsuario))
            .encode()
            .build()
            .toUriString();
        log.info("URL de autorizacion OAuth para usuario {}: {}",
            idUsuario, url);
        return url;
    }

    public String redirectUri() {
        return frontendUrl + "/api/pagos/mercadopago/oauth/callback";
    }

    @Transactional
    public void procesarCallback(String codigo, String stateRecibido) {
        Long idUsuario = state.validar(stateRecibido);
        Usuario usuario = usuarios.findById(idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idUsuario));
        MercadoPagoToken token = cliente.intercambiarCodigo(
            codigo, redirectUri(), oauthTestToken);
        if (token.accessToken() == null || token.accessToken().isBlank()) {
            throw new MercadoPagoException(
                "MercadoPago no devolvió un access token.");
        }

        CuentaPagoUsuario cuenta = cuentas
            .findByUsuarioIdUsuarioAndProveedor(idUsuario, PROVEEDOR)
            .orElseGet(() -> {
                CuentaPagoUsuario nueva = new CuentaPagoUsuario();
                nueva.setUsuario(usuario);
                nueva.setProveedor(PROVEEDOR);
                return nueva;
            });
        cuenta.setIdCuentaExterna(token.userId() != null
            ? String.valueOf(token.userId())
            : null);
        cuenta.setAccessToken(cifrador.cifrar(token.accessToken()));
        cuenta.setRefreshToken(cifrador.cifrar(token.refreshToken()));
        cuenta.setTokenExpiraEn(LocalDateTime.now().plusSeconds(
            token.expiresIn() != null ? token.expiresIn() : 3600));
        cuenta.setEstado(EstadoCuentaPago.ACTIVA);
        cuenta.setFechaActualizacion(LocalDateTime.now());
        cuentas.save(cuenta);
    }

    @Transactional(readOnly = true)
    public CuentaCobroDTO estado(Long idUsuario) {
        return cuentas
            .findByUsuarioIdUsuarioAndProveedor(idUsuario, PROVEEDOR)
            .map(cuenta -> new CuentaCobroDTO(
                cuenta.getEstado() == EstadoCuentaPago.ACTIVA,
                cuenta.getEstado() != null ? cuenta.getEstado().name() : null))
            .orElse(new CuentaCobroDTO(false, null));
    }

    @Transactional
    public String tokenVigente(Long idUsuario) {
        CuentaPagoUsuario cuenta = cuentas
            .findByUsuarioIdUsuarioAndProveedor(idUsuario, PROVEEDOR)
            .orElseThrow(() -> new ConflictoException(
                "El artista todavía no conectó su cuenta de MercadoPago."));
        if (cuenta.getEstado() != EstadoCuentaPago.ACTIVA) {
            throw new ConflictoException(
                "La cuenta de MercadoPago del artista no está activa.");
        }
        if (necesitaRefresco(cuenta)) {
            refrescar(cuenta);
        }
        return cifrador.descifrar(cuenta.getAccessToken());
    }

    /**
     * Tokens de todas las cuentas activas. Se usa cuando el dueño del cobro no
     * se conoce de antemano: en marketplace el pago pertenece al vendedor, así
     * que el token de la plataforma no siempre puede leerlo.
     */
    @Transactional
    public List<String> tokensVigentes() {
        List<String> tokens = new ArrayList<>();
        for (CuentaPagoUsuario cuenta : cuentas.findByProveedorAndEstado(
                PROVEEDOR, EstadoCuentaPago.ACTIVA)) {
            try {
                if (necesitaRefresco(cuenta)) {
                    refrescar(cuenta);
                }
                tokens.add(cifrador.descifrar(cuenta.getAccessToken()));
            } catch (RuntimeException e) {
                log.warn("No se pudo obtener el token de la cuenta {}: {}",
                    cuenta.getIdCuentaPago(), e.getMessage());
            }
        }
        return tokens;
    }

    private boolean necesitaRefresco(CuentaPagoUsuario cuenta) {
        return cuenta.getAccessToken() == null
            || cuenta.getTokenExpiraEn() == null
            || cuenta.getTokenExpiraEn()
                .isBefore(LocalDateTime.now().plusMinutes(5));
    }

    private void refrescar(CuentaPagoUsuario cuenta) {
        try {
            if (cuenta.getRefreshToken() == null) {
                throw new IllegalStateException("Sin refresh token.");
            }
            MercadoPagoToken token = cliente.refrescarToken(
                cifrador.descifrar(cuenta.getRefreshToken()));
            cuenta.setAccessToken(cifrador.cifrar(token.accessToken()));
            if (token.refreshToken() != null
                    && !token.refreshToken().isBlank()) {
                cuenta.setRefreshToken(
                    cifrador.cifrar(token.refreshToken()));
            }
            cuenta.setTokenExpiraEn(LocalDateTime.now().plusSeconds(
                token.expiresIn() != null ? token.expiresIn() : 3600));
            cuenta.setFechaActualizacion(LocalDateTime.now());
            cuentas.save(cuenta);
        } catch (RuntimeException e) {
            cuenta.setEstado(EstadoCuentaPago.ERROR);
            cuenta.setFechaActualizacion(LocalDateTime.now());
            cuentas.save(cuenta);
            throw new ConflictoException(
                "La conexión con MercadoPago expiró. "
                    + "Volvé a conectar tu cuenta.");
        }
    }

    @Transactional
    public void desconectar(Long idUsuario) {
        cuentas.findByUsuarioIdUsuarioAndProveedor(idUsuario, PROVEEDOR)
            .ifPresent(cuenta -> {
                cuenta.setEstado(EstadoCuentaPago.REVOCADA);
                cuenta.setAccessToken(null);
                cuenta.setRefreshToken(null);
                cuenta.setTokenExpiraEn(null);
                cuenta.setFechaActualizacion(LocalDateTime.now());
                cuentas.save(cuenta);
            });
    }
}
