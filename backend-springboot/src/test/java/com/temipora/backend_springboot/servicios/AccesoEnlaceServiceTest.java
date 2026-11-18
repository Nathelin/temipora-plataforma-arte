package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.config.ContactoPlataforma;
import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.PropositoToken;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.TokenAcceso;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;
import com.temipora.backend_springboot.repositorios.TokenAccesoRepository;

@ExtendWith(MockitoExtension.class)
class AccesoEnlaceServiceTest {

    private static final String TOKEN = "token-crudo";

    @Mock
    private TokenAccesoRepository tokenRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuditoriaService auditoria;

    @Mock
    private SancionCuentaRepository sancionCuentaRepository;

    @Mock
    private ContactoPlataforma contacto;

    @InjectMocks
    private AccesoEnlaceService service;

    @Test
    void consumir_valido_marcaUsadoYDevuelveLaSesion() {
        TokenAcceso token = token(usuario(true), LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHashForUpdate(
            TokenAccesoUtil.hash(TOKEN))).thenReturn(Optional.of(token));
        when(jwtUtils.generarTokenAcceso("cliente", "CLIENTE"))
            .thenReturn("jwt");

        Map<String, Object> sesion = service.consumir(TOKEN);

        assertThat(sesion.get("token")).isEqualTo("jwt");
        assertThat(sesion.get("NombreUsuario")).isEqualTo("cliente");
        assertThat(sesion.get("rol")).isEqualTo("CLIENTE");
        assertThat(sesion.get("proposito")).isEqualTo("OFERTA");
        assertThat(token.getUsadoEn()).isNotNull();
        verify(usuarioService).actualizarUltimaConexion("cliente");
        verify(auditoria).registrarAccesoEnlace("cliente", "OFERTA", true, null);
    }

    @Test
    void consumir_yaUsado_lanzaErrorYAudita() {
        TokenAcceso token = token(usuario(true), LocalDateTime.now().plusHours(1));
        token.setUsadoEn(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByTokenHashForUpdate(
            TokenAccesoUtil.hash(TOKEN))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consumir(TOKEN))
            .isInstanceOf(AccesoEnlaceInvalidoException.class)
            .hasMessageContaining("ya fue usado");
        verify(auditoria).registrarAccesoEnlace(
            "cliente", "OFERTA", false, "Enlace ya usado");
    }

    @Test
    void consumir_vencido_lanzaError() {
        TokenAcceso token = token(usuario(true), LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByTokenHashForUpdate(
            TokenAccesoUtil.hash(TOKEN))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consumir(TOKEN))
            .isInstanceOf(AccesoEnlaceInvalidoException.class)
            .hasMessageContaining("venció");
    }

    @Test
    void consumir_cuentaInactiva_lanzaError() {
        TokenAcceso token = token(usuario(false), LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHashForUpdate(
            TokenAccesoUtil.hash(TOKEN))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consumir(TOKEN))
            .isInstanceOf(AccesoEnlaceInvalidoException.class)
            .hasMessageContaining("no está activa");
    }

    @Test
    void consumir_tokenDesconocido_lanzaError() {
        when(tokenRepository.findByTokenHashForUpdate(any()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consumir(TOKEN))
            .isInstanceOf(AccesoEnlaceInvalidoException.class)
            .hasMessageContaining("no es válido");
        verify(auditoria).registrarAccesoEnlace(null, null, false, "Token inválido");
    }

    @Test
    void generar_guardaElHashYElVencimiento() {
        when(tokenRepository.save(any(TokenAcceso.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        String crudo = service.generar(
            usuario(true), PropositoToken.CIERRE, 77L, 24);

        assertThat(crudo).isNotBlank();
        ArgumentCaptor<TokenAcceso> captor =
            ArgumentCaptor.forClass(TokenAcceso.class);
        verify(tokenRepository).save(captor.capture());
        TokenAcceso guardado = captor.getValue();
        assertThat(guardado.getTokenHash()).hasSize(64).isNotEqualTo(crudo);
        assertThat(guardado.getProposito()).isEqualTo(PropositoToken.CIERRE);
        assertThat(guardado.getIdReferencia()).isEqualTo(77L);
        assertThat(guardado.getExpira()).isAfter(LocalDateTime.now().plusHours(23));
    }

    private TokenAcceso token(Usuario usuario, LocalDateTime expira) {
        TokenAcceso token = new TokenAcceso();
        token.setUsuario(usuario);
        token.setProposito(PropositoToken.OFERTA);
        token.setTokenHash(TokenAccesoUtil.hash(TOKEN));
        token.setExpira(expira);
        return token;
    }

    private Usuario usuario(boolean activo) {
        Rol rol = new Rol();
        rol.setNombre(ERol.CLIENTE);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setNombreUsuario("cliente");
        usuario.setEstadoCuenta(activo);
        usuario.setRol(rol);
        return usuario;
    }
}
