package com.temipora.backend_springboot.config.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.config.SecurityConfig;
import com.temipora.backend_springboot.controladores.AuthController;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.BajaCuentaService;
import com.temipora.backend_springboot.servicios.LimpiezaArchivosService;
import com.temipora.backend_springboot.servicios.StorageService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = AuthController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class JwtAuthenticationFilterTest {

    private static final String URL_LOGIN = "/api/auth/login";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private LimpiezaArchivosService limpiezaArchivosService;

    @MockitoBean
    private BajaCuentaService bajaCuentaService;

    // ---------------------------------------------------------------
    // POST /api/auth/login
    // ---------------------------------------------------------------

    @Test
    void login_credencialesValidas_devuelve200ConTokenFirmado() throws Exception {
        when(userDetailsService.loadUserByUsername("artista777"))
                .thenReturn(User.withUsername("artista777")
                        .password(passwordEncoder.encode("secreta123"))
                        .authorities("ROLE_ARTISTA")
                        .build());
        when(usuarioService.getByNombreUsuario("artista777"))
                .thenReturn(Usuario.builder()
                        .idUsuario(7L)
                        .nombreUsuario("artista777")
                        .rol(Rol.builder().nombre(ERol.ARTISTA).build())
                        .urlFotoPerfil("uploads/pfp.png")
                        .build());

        MvcResult resultado = mockMvc.perform(post(URL_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"artista777\",\"contrasenia\":\"secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.NombreUsuario").value("artista777"))
                .andExpect(jsonPath("$.rol").value("ARTISTA"))
                .andExpect(jsonPath("$.idUsuario").value(7))
                .andExpect(jsonPath("$.urlFotoPerfil").value("uploads/pfp.png"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = JsonPath.read(
                resultado.getResponse().getContentAsString(), "$.token");
        assertThat(jwtUtils.tokenEsValido(token)).isTrue();

        verify(usuarioService).actualizarUltimaConexion("artista777");
    }

    @Test
    void login_contraseniaIncorrecta_devuelve401SinActualizarUltimaConexion() throws Exception {
        when(userDetailsService.loadUserByUsername("artista777"))
                .thenReturn(User.withUsername("artista777")
                        .password(passwordEncoder.encode("secreta123"))
                        .authorities("ROLE_ARTISTA")
                        .build());

        mockMvc.perform(post(URL_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"artista777\",\"contrasenia\":\"otraClave\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", containsString("Credenciales")));

        verify(usuarioService, never()).actualizarUltimaConexion(any());
        verify(usuarioService, never()).getByNombreUsuario(any());
    }

    @Test
    void login_usuarioInexistente_devuelve401() throws Exception {
        when(userDetailsService.loadUserByUsername("nadie"))
                .thenThrow(new UsernameNotFoundException("nadie"));

        mockMvc.perform(post(URL_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"nadie\",\"contrasenia\":\"secreta123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Credenciales")));

        verify(usuarioService, never()).actualizarUltimaConexion(any());
        verify(usuarioService, never()).getByNombreUsuario(any());
    }

    @Test
    void login_bodyMalformado_devuelve401() throws Exception {
        when(userDetailsService.loadUserByUsername(""))
                .thenThrow(new UsernameNotFoundException("vacio"));

        mockMvc.perform(post(URL_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{nombreUsuario"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Credenciales")));

        verify(usuarioService, never()).actualizarUltimaConexion(any());
    }

    @Test
    void login_sancionadoConCredencialesValidas_devuelve403ConMotivoYContacto() throws Exception {
        when(userDetailsService.loadUserByUsername("sancionado"))
                .thenReturn(User.withUsername("sancionado")
                        .password(passwordEncoder.encode("secreta123"))
                        .authorities("ROLE_CLIENTE")
                        .build());
        when(usuarioService.getByNombreUsuario("sancionado"))
                .thenReturn(Usuario.builder()
                        .idUsuario(8L)
                        .nombreUsuario("sancionado")
                        .rol(Rol.builder().nombre(ERol.CLIENTE).build())
                        .build());
        when(usuarioService.sancionVigente(8L))
                .thenReturn(Optional.of(SancionCuenta.builder()
                        .idSancion(1L)
                        .motivo("Spam reiterado")
                        .fechaFin(LocalDateTime.of(2026, 12, 1, 10, 0))
                        .build()));

        mockMvc.perform(post(URL_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"sancionado\",\"contrasenia\":\"secreta123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_SANCIONADA"))
                .andExpect(jsonPath("$.motivo").value("Spam reiterado"))
                .andExpect(jsonPath("$.contacto").value("temiporasupport@gmail.com"))
                .andExpect(jsonPath("$.token").doesNotExist());

        verify(usuarioService, never()).actualizarUltimaConexion(any());
    }

    @Test
    void login_sancionadoConContraseniaIncorrecta_devuelve401SinRevelarLaSancion() throws Exception {
        when(userDetailsService.loadUserByUsername("sancionado"))
                .thenReturn(User.withUsername("sancionado")
                        .password(passwordEncoder.encode("secreta123"))
                        .authorities("ROLE_CLIENTE")
                        .build());

        mockMvc.perform(post(URL_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"sancionado\",\"contrasenia\":\"otraClave\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Credenciales")));

        verify(usuarioService, never()).sancionVigente(any());
        verify(usuarioService, never()).actualizarUltimaConexion(any());
    }

    // ---------------------------------------------------------------
    // Cuenta dada de baja voluntariamente
    // ---------------------------------------------------------------

    @Test
    void login_cuentaDeBajaConCredencialesValidas_devuelve403ConDatosDeReactivacion() throws Exception {
        Usuario usuario = usuarioDeBaja(LocalDateTime.now().minusDays(2));
        stubUsuarioDeBaja(usuario);

        mockMvc.perform(post(URL_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"baja\",\"contrasenia\":\"secreta123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_DE_BAJA"))
                .andExpect(jsonPath("$.puedeReactivar").value(true))
                .andExpect(jsonPath("$.fechaBaja").isNotEmpty())
                .andExpect(jsonPath("$.reactivarHasta").isNotEmpty())
                .andExpect(jsonPath("$.contacto").value("temiporasupport@gmail.com"))
                .andExpect(jsonPath("$.token").doesNotExist());

        verify(usuarioService, never()).actualizarUltimaConexion(any());
    }

    @Test
    void login_cuentaDeBajaVencida_devuelve403SinReactivacion() throws Exception {
        Usuario usuario = usuarioDeBaja(LocalDateTime.now().minusDays(40));
        stubUsuarioDeBaja(usuario);

        mockMvc.perform(post(URL_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"baja\",\"contrasenia\":\"secreta123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_DE_BAJA"))
                .andExpect(jsonPath("$.puedeReactivar").value(false))
                .andExpect(jsonPath("$.reactivarHasta").isNotEmpty());
    }

    @Test
    void login_cuentaDeBajaConContraseniaIncorrecta_devuelve401SinRevelarElEstado() throws Exception {
        Usuario usuario = usuarioDeBaja(LocalDateTime.now().minusDays(2));
        stubUsuarioDeBaja(usuario);

        mockMvc.perform(post(URL_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"baja\",\"contrasenia\":\"otraClave\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Credenciales")))
                .andExpect(jsonPath("$.codigo").doesNotExist());
    }

    @Test
    void login_cuentaInactivaSinBaja_devuelve403CuentaInactiva() throws Exception {
        Usuario usuario = usuarioDeBaja(null);
        stubUsuarioDeBaja(usuario);

        mockMvc.perform(post(URL_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreUsuario\":\"baja\",\"contrasenia\":\"secreta123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CUENTA_INACTIVA"));
    }

    private Usuario usuarioDeBaja(LocalDateTime fechaBaja) {
        return Usuario.builder()
                .idUsuario(9L)
                .nombreUsuario("baja")
                .contrasenia(passwordEncoder.encode("secreta123"))
                .estadoCuenta(false)
                .fechaBajaVoluntaria(fechaBaja)
                .rol(Rol.builder().nombre(ERol.CLIENTE).build())
                .build();
    }

    private void stubUsuarioDeBaja(Usuario usuario) {
        when(userDetailsService.loadUserByUsername("baja"))
                .thenReturn(User.withUsername("baja")
                        .password(usuario.getContrasenia())
                        .disabled(true)
                        .authorities("ROLE_CLIENTE")
                        .build());
        when(usuarioService.getByNombreUsuario("baja")).thenReturn(usuario);
    }
}
