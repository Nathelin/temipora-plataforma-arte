package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.modelo.CampanaReactivacion;
import com.temipora.backend_springboot.modelo.EstadoCampanaReactivacion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CampanaReactivacionRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccesoOfertaServiceTest {

    private static final String TOKEN = "token-secreto";
    private static final String TOKEN_CAMPANA = "campana-123";

    @Mock
    private CampanaReactivacionRepository campanaRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AccesoOfertaService service;

    private CampanaReactivacion campana;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7L);
        usuario.setNombreUsuario("ana");
        usuario.setEstadoCuenta(true);
        usuario.setUrlFotoPerfil("/ana.png");

        campana = new CampanaReactivacion();
        campana.setIdCampana(3L);
        campana.setToken(TOKEN_CAMPANA);
        campana.setUsuario(usuario);
        campana.setEstado(EstadoCampanaReactivacion.ACTIVA);
        campana.setFechaVencimiento(LocalDateTime.now().plusHours(2));
        campana.setTokenAccesoHash(TokenAccesoOferta.hash(TOKEN));
        campana.setTokenAccesoExpira(LocalDateTime.now().plusHours(2));
    }

    @Test
    void verificar_devuelveUsuarioSinConsumir() {
        when(campanaRepository.findByToken(TOKEN_CAMPANA))
            .thenReturn(Optional.of(campana));

        var dto = service.verificar(TOKEN_CAMPANA, TOKEN);

        assertThat(dto.idUsuario()).isEqualTo(7L);
        assertThat(dto.nombreUsuario()).isEqualTo("ana");
        assertThat(campana.getTokenAccesoUsadoEn()).isNull();
        verify(campanaRepository, never()).save(any());
    }

    @Test
    void verificar_rechazaTokenInvalido() {
        when(campanaRepository.findByToken(TOKEN_CAMPANA))
            .thenReturn(Optional.of(campana));

        assertThatThrownBy(() -> service.verificar(TOKEN_CAMPANA, "otro"))
            .isInstanceOf(AccesoOfertaInvalidoException.class)
            .hasMessageContaining("no es válido");
    }

    @Test
    void verificar_rechazaTokenUsado() {
        campana.setTokenAccesoUsadoEn(LocalDateTime.now());
        when(campanaRepository.findByToken(TOKEN_CAMPANA))
            .thenReturn(Optional.of(campana));

        assertThatThrownBy(() -> service.verificar(TOKEN_CAMPANA, TOKEN))
            .isInstanceOf(AccesoOfertaInvalidoException.class)
            .hasMessageContaining("ya fue usado");
    }

    @Test
    void verificar_rechazaTokenVencido() {
        campana.setTokenAccesoExpira(LocalDateTime.now().minusMinutes(1));
        when(campanaRepository.findByToken(TOKEN_CAMPANA))
            .thenReturn(Optional.of(campana));

        assertThatThrownBy(() -> service.verificar(TOKEN_CAMPANA, TOKEN))
            .isInstanceOf(AccesoOfertaInvalidoException.class)
            .hasMessageContaining("venció");
    }

    @Test
    void consumir_marcaUsadoYDevuelveSesion() {
        when(campanaRepository.findByTokenParaActualizar(TOKEN_CAMPANA))
            .thenReturn(Optional.of(campana));
        when(jwtUtils.generarTokenAcceso("ana", "")).thenReturn("jwt");

        Map<String, Object> respuesta = service.consumir(TOKEN_CAMPANA, TOKEN);

        assertThat(respuesta)
            .containsEntry("token", "jwt")
            .containsEntry("NombreUsuario", "ana")
            .containsEntry("idUsuario", 7L)
            .containsEntry("rol", "")
            .containsEntry("urlFotoPerfil", "/ana.png");
        assertThat(campana.getTokenAccesoUsadoEn()).isNotNull();
        verify(usuarioService).actualizarUltimaConexion("ana");
        verify(campanaRepository).save(campana);
    }
}
