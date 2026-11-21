package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.temipora.backend_springboot.config.ContactoPlataforma;
import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.modelo.BajaCuentaResultadoDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class BajaCuentaServiceTest {

    private static final String CLAVE = "Secreta123";
    private static final String HASH = "$2a$10$hashDePrueba";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ConfiguracionBajaCuentaService configuracionBajaCuentaService;

    @Mock
    private AuditoriaService auditoria;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private ContactoPlataforma contacto;

    @Mock
    private SancionService sancionService;

    @InjectMocks
    private BajaCuentaService bajaCuentaService;

    // ---------------------------------------------------------------
    // Dar de baja
    // ---------------------------------------------------------------

    @Test
    void darDeBaja_conPedidoYSolicitudesAbiertas_lanzaConflictoYNoDesactiva() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(CLAVE, HASH)).thenReturn(true);
        when(pedidoRepository.countByClienteIdUsuarioAndEstadoIn(eq(7L), any()))
            .thenReturn(1L);
        when(pedidoRepository.countByArtistaIdUsuarioAndEstadoIn(eq(7L), any()))
            .thenReturn(0L);
        when(solicitudRepository.countByClienteIdUsuarioAndEstadoIn(eq(7L), any()))
            .thenReturn(0L);
        when(solicitudRepository.countByArtistaIdUsuarioAndEstadoIn(eq(7L), any()))
            .thenReturn(2L);

        assertThatThrownBy(() -> bajaCuentaService.darDeBaja(7L, CLAVE))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("1 pedido abierto")
            .hasMessageContaining("2 solicitudes abiertas");

        assertThat(usuario.isEstadoCuenta()).isTrue();
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void darDeBaja_sinCompromisos_marcaFechaYDevuelveElPlazo() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(CLAVE, HASH)).thenReturn(true);
        when(pedidoRepository.countByClienteIdUsuarioAndEstadoIn(eq(7L), any()))
            .thenReturn(0L);
        when(pedidoRepository.countByArtistaIdUsuarioAndEstadoIn(eq(7L), any()))
            .thenReturn(0L);
        when(solicitudRepository.countByClienteIdUsuarioAndEstadoIn(eq(7L), any()))
            .thenReturn(0L);
        when(solicitudRepository.countByArtistaIdUsuarioAndEstadoIn(eq(7L), any()))
            .thenReturn(0L);
        when(configuracionBajaCuentaService.diasParaReactivar()).thenReturn(30);

        BajaCuentaResultadoDTO resultado = bajaCuentaService.darDeBaja(7L, CLAVE);

        assertThat(usuario.isEstadoCuenta()).isFalse();
        assertThat(usuario.getFechaBajaVoluntaria()).isNotNull();
        assertThat(resultado.getReactivarHasta())
            .isAfter(LocalDateTime.now().plusDays(29))
            .isBefore(LocalDateTime.now().plusDays(31));
        verify(usuarioRepository).save(usuario);
        verify(auditoria).registrarCambios(
            eq(AccionesAdmin.USUARIO_BAJA_VOLUNTARIA), eq("Usuario"), eq(7L),
            anyString(), anyList());
    }

    @Test
    void darDeBaja_contraseniaIncorrecta_lanzaErrorYNoDesactiva() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("otra", HASH)).thenReturn(false);

        assertThatThrownBy(() -> bajaCuentaService.darDeBaja(7L, "otra"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La contraseña actual no es correcta.");

        assertThat(usuario.isEstadoCuenta()).isTrue();
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void darDeBaja_administrador_lanzaConflicto() {
        Usuario admin = usuarioActivo();
        admin.setRol(Rol.builder().idRol(1L).nombre(ERol.ADMIN).build());
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches(CLAVE, HASH)).thenReturn(true);

        assertThatThrownBy(() -> bajaCuentaService.darDeBaja(7L, CLAVE))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("administración");

        assertThat(admin.isEstadoCuenta()).isTrue();
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void darDeBaja_cuentaYaInactiva_lanzaConflicto() {
        Usuario usuario = usuarioActivo();
        usuario.setEstadoCuenta(false);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(CLAVE, HASH)).thenReturn(true);

        assertThatThrownBy(() -> bajaCuentaService.darDeBaja(7L, CLAVE))
            .isInstanceOf(ConflictoException.class)
            .hasMessage("Tu cuenta ya está inactiva.");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // ---------------------------------------------------------------
    // Reactivación con credenciales
    // ---------------------------------------------------------------

    @Test
    void reactivarConCredenciales_dentroDelPlazo_activaYDevuelveSesion() {
        Usuario usuario = usuarioInactivo(LocalDateTime.now().minusDays(5));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(CLAVE, HASH)).thenReturn(true);
        when(configuracionBajaCuentaService.diasParaReactivar()).thenReturn(30);
        when(jwtUtils.generarTokenAcceso("cliente", "CLIENTE"))
            .thenReturn("token-123");

        Map<String, Object> respuesta = bajaCuentaService
            .reactivarConCredenciales(" cliente ", CLAVE);

        assertThat(respuesta.get("token")).isEqualTo("token-123");
        assertThat(respuesta.get("rol")).isEqualTo("CLIENTE");
        assertThat(usuario.isEstadoCuenta()).isTrue();
        assertThat(usuario.getFechaBajaVoluntaria()).isNull();
        verify(usuarioRepository).save(usuario);
        verify(auditoria).registrarActor(
            eq(AccionesAdmin.USUARIO_REACTIVAR_CUENTA), eq("Usuario"), eq(7L),
            anyString(), eq("cliente"), eq("CLIENTE"));
    }

    @Test
    void reactivarConCredenciales_fueraDelPlazo_lanzaReactivacionVencida() {
        Usuario usuario = usuarioInactivo(LocalDateTime.now().minusDays(40));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(CLAVE, HASH)).thenReturn(true);
        when(configuracionBajaCuentaService.diasParaReactivar()).thenReturn(30);
        when(contacto.getEmail()).thenReturn("soporte@temipora.com");

        assertThatThrownBy(() -> bajaCuentaService
            .reactivarConCredenciales("cliente", CLAVE))
            .isInstanceOf(ReactivacionVencidaException.class)
            .hasMessage("El plazo para reactivar la cuenta venció.");

        assertThat(usuario.isEstadoCuenta()).isFalse();
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void reactivarConCredenciales_contraseniaIncorrecta_lanzaCredencialesInvalidas() {
        Usuario usuario = usuarioInactivo(LocalDateTime.now().minusDays(5));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("otra", HASH)).thenReturn(false);

        assertThatThrownBy(() -> bajaCuentaService
            .reactivarConCredenciales("cliente", "otra"))
            .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void reactivarConCredenciales_cuentaActiva_lanzaConflicto() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(CLAVE, HASH)).thenReturn(true);

        assertThatThrownBy(() -> bajaCuentaService
            .reactivarConCredenciales("cliente", CLAVE))
            .isInstanceOf(ConflictoException.class)
            .hasMessage("La cuenta ya está activa.");
    }

    @Test
    void reactivarConCredenciales_cuentaInactivaSinBaja_lanzaConflicto() {
        Usuario usuario = usuarioInactivo(null);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(CLAVE, HASH)).thenReturn(true);
        when(contacto.getEmail()).thenReturn("soporte@temipora.com");

        assertThatThrownBy(() -> bajaCuentaService
            .reactivarConCredenciales("cliente", CLAVE))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("no está dada de baja voluntariamente");
    }

    // ---------------------------------------------------------------
    // Reactivación desde administración
    // ---------------------------------------------------------------

    @Test
    void reactivarComoAdmin_activaLaCuenta() {
        Usuario usuario = usuarioInactivo(LocalDateTime.now().minusDays(40));
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(usuario));
        when(configuracionBajaCuentaService.diasParaReactivar()).thenReturn(30);

        bajaCuentaService.reactivarComoAdmin(7L);

        assertThat(usuario.isEstadoCuenta()).isTrue();
        assertThat(usuario.getFechaBajaVoluntaria()).isNull();
        verify(usuarioRepository).save(usuario);
        verify(auditoria).registrarCambios(
            eq(AccionesAdmin.USUARIO_REACTIVAR_CUENTA_ADMIN), eq("Usuario"), eq(7L),
            anyString(), anyList());
    }

    // ---------------------------------------------------------------
    // Parámetro vigente
    // ---------------------------------------------------------------

    @Test
    void reactivarHasta_usaElValorVigenteDeLaConfiguracion() {
        Usuario usuario = usuarioInactivo(LocalDateTime.now().minusDays(10));
        when(configuracionBajaCuentaService.diasParaReactivar()).thenReturn(30);
        LocalDateTime conTreinta = bajaCuentaService.reactivarHasta(usuario);

        when(configuracionBajaCuentaService.diasParaReactivar()).thenReturn(45);
        LocalDateTime conCuarentaYCinco = bajaCuentaService.reactivarHasta(usuario);

        assertThat(conCuarentaYCinco).isAfter(conTreinta);
        assertThat(conCuarentaYCinco)
            .isAfter(LocalDateTime.now().plusDays(34))
            .isBefore(LocalDateTime.now().plusDays(36));
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Usuario usuarioActivo() {
        return Usuario.builder()
            .idUsuario(7L)
            .nombreUsuario("cliente")
            .correo("cliente@mail.com")
            .contrasenia(HASH)
            .estadoCuenta(true)
            .rol(Rol.builder().idRol(3L).nombre(ERol.CLIENTE).build())
            .build();
    }

    private Usuario usuarioInactivo(LocalDateTime fechaBaja) {
        return Usuario.builder()
            .idUsuario(7L)
            .nombreUsuario("cliente")
            .correo("cliente@mail.com")
            .contrasenia(HASH)
            .estadoCuenta(false)
            .fechaBajaVoluntaria(fechaBaja)
            .rol(Rol.builder().idRol(3L).nombre(ERol.CLIENTE).build())
            .build();
    }
}
