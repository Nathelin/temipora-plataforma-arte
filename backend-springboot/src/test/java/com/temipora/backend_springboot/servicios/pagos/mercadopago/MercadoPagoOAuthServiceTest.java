package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.CuentaCobroDTO;
import com.temipora.backend_springboot.modelo.CuentaPagoUsuario;
import com.temipora.backend_springboot.modelo.EstadoCuentaPago;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CuentaPagoUsuarioRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.ConflictoException;

@ExtendWith(MockitoExtension.class)
class MercadoPagoOAuthServiceTest {

    private static final String REDIRECT =
        "https://front.test/api/pagos/mercadopago/oauth/callback";

    @Mock
    private MercadoPagoClient cliente;
    @Mock
    private MercadoPagoOAuthState state;
    @Mock
    private CuentaPagoUsuarioRepository cuentas;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private CifradorTokens cifrador;

    private MercadoPagoOAuthService service;

    @BeforeEach
    void setUp() {
        service = new MercadoPagoOAuthService(
            cliente,
            state,
            cuentas,
            usuarios,
            cifrador,
            "client-id",
            "https://auth.test/authorization",
            "https://front.test",
            true);
    }

    @Test
    void urlAutorizacion_incluyeCredencialesYState() {
        when(state.generar(5L)).thenReturn("state-5");

        String url = service.urlAutorizacion(5L);

        assertThat(url).contains("client_id=client-id");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("platform_id=mp");
        assertThat(url).contains("state=state-5");
        assertThat(url).contains("redirect_uri=");
        assertThat(url).contains("front.test");
    }

    @Test
    void procesarCallback_guardaLosTokensCifrados() {
        when(state.validar("state")).thenReturn(5L);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(5L);
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(cliente.intercambiarCodigo("code", REDIRECT, true))
            .thenReturn(new MercadoPagoToken(
                "AT", "RT", 3600L, 123L, "offline_access", "bearer"));
        when(cuentas.findByUsuarioIdUsuarioAndProveedor(5L, "MERCADOPAGO"))
            .thenReturn(Optional.empty());
        when(cifrador.cifrar("AT")).thenReturn("encAT");
        when(cifrador.cifrar("RT")).thenReturn("encRT");

        service.procesarCallback("code", "state");

        ArgumentCaptor<CuentaPagoUsuario> captor =
            ArgumentCaptor.forClass(CuentaPagoUsuario.class);
        verify(cuentas).save(captor.capture());
        CuentaPagoUsuario guardada = captor.getValue();
        assertThat(guardada.getUsuario()).isSameAs(usuario);
        assertThat(guardada.getProveedor()).isEqualTo("MERCADOPAGO");
        assertThat(guardada.getIdCuentaExterna()).isEqualTo("123");
        assertThat(guardada.getAccessToken()).isEqualTo("encAT");
        assertThat(guardada.getRefreshToken()).isEqualTo("encRT");
        assertThat(guardada.getEstado()).isEqualTo(EstadoCuentaPago.ACTIVA);
        assertThat(guardada.getTokenExpiraEn()).isAfter(LocalDateTime.now());
    }

    @Test
    void procesarCallback_sinTestToken_canjeaSinTokenDePrueba() {
        MercadoPagoOAuthService sinTestToken = new MercadoPagoOAuthService(
            cliente,
            state,
            cuentas,
            usuarios,
            cifrador,
            "client-id",
            "https://auth.test/authorization",
            "https://front.test",
            false);
        when(state.validar("state")).thenReturn(5L);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(5L);
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(cliente.intercambiarCodigo("code", REDIRECT, false))
            .thenReturn(new MercadoPagoToken(
                "AT", "RT", 3600L, 123L, "offline_access", "bearer"));
        when(cuentas.findByUsuarioIdUsuarioAndProveedor(5L, "MERCADOPAGO"))
            .thenReturn(Optional.empty());
        when(cifrador.cifrar("AT")).thenReturn("encAT");
        when(cifrador.cifrar("RT")).thenReturn("encRT");

        sinTestToken.procesarCallback("code", "state");

        verify(cliente).intercambiarCodigo("code", REDIRECT, false);
    }

    @Test
    void tokenVigente_devuelveElTokenDescifrado() {
        CuentaPagoUsuario cuenta = cuenta();
        when(cuentas.findByUsuarioIdUsuarioAndProveedor(5L, "MERCADOPAGO"))
            .thenReturn(Optional.of(cuenta));
        when(cifrador.descifrar("encAT")).thenReturn("AT");

        assertThat(service.tokenVigente(5L)).isEqualTo("AT");
    }

    @Test
    void tokenVigente_renuevaSiEstaPorExpirar() {
        CuentaPagoUsuario cuenta = cuenta();
        cuenta.setTokenExpiraEn(LocalDateTime.now().minusMinutes(1));
        when(cuentas.findByUsuarioIdUsuarioAndProveedor(5L, "MERCADOPAGO"))
            .thenReturn(Optional.of(cuenta));
        when(cifrador.descifrar("encRT")).thenReturn("RT");
        when(cliente.refrescarToken("RT")).thenReturn(
            new MercadoPagoToken(
                "AT2", "RT2", 3600L, 123L, "offline_access", "bearer"));
        when(cifrador.cifrar("AT2")).thenReturn("encAT2");
        when(cifrador.cifrar("RT2")).thenReturn("encRT2");
        when(cifrador.descifrar("encAT2")).thenReturn("AT2");

        assertThat(service.tokenVigente(5L)).isEqualTo("AT2");
        verify(cuentas).save(cuenta);
    }

    @Test
    void tokenVigente_sinCuenta_lanzaConflicto() {
        when(cuentas.findByUsuarioIdUsuarioAndProveedor(5L, "MERCADOPAGO"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.tokenVigente(5L))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("no conectó");
    }

    @Test
    void tokensVigentes_devuelveSoloLosDeLasCuentasQueSePuedenDescifrar() {
        CuentaPagoUsuario buena = cuenta();
        CuentaPagoUsuario rota = cuenta();
        rota.setAccessToken("encRoto");
        when(cuentas.findByProveedorAndEstado(
                "MERCADOPAGO", EstadoCuentaPago.ACTIVA))
            .thenReturn(List.of(buena, rota));
        when(cifrador.descifrar("encAT")).thenReturn("AT");
        when(cifrador.descifrar("encRoto"))
            .thenThrow(new IllegalStateException("Token cifrado inválido."));

        assertThat(service.tokensVigentes()).containsExactly("AT");
    }

    @Test
    void estado_sinCuenta_devuelveNoConectada() {
        when(cuentas.findByUsuarioIdUsuarioAndProveedor(5L, "MERCADOPAGO"))
            .thenReturn(Optional.empty());

        CuentaCobroDTO estado = service.estado(5L);

        assertThat(estado.conectada()).isFalse();
    }

    @Test
    void desconectar_revocaYlimpiaLosTokens() {
        CuentaPagoUsuario cuenta = cuenta();
        when(cuentas.findByUsuarioIdUsuarioAndProveedor(5L, "MERCADOPAGO"))
            .thenReturn(Optional.of(cuenta));

        service.desconectar(5L);

        assertThat(cuenta.getEstado())
            .isEqualTo(EstadoCuentaPago.REVOCADA);
        assertThat(cuenta.getAccessToken()).isNull();
        assertThat(cuenta.getRefreshToken()).isNull();
        verify(cuentas).save(cuenta);
    }

    private CuentaPagoUsuario cuenta() {
        CuentaPagoUsuario cuenta = new CuentaPagoUsuario();
        cuenta.setEstado(EstadoCuentaPago.ACTIVA);
        cuenta.setAccessToken("encAT");
        cuenta.setRefreshToken("encRT");
        cuenta.setTokenExpiraEn(LocalDateTime.now().plusHours(1));
        return cuenta;
    }
}
