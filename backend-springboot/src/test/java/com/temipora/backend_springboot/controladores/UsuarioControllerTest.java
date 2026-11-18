package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.config.SecurityConfig;
import com.temipora.backend_springboot.modelo.ActualizarCorreoDTO;
import com.temipora.backend_springboot.modelo.ActualizarPerfilDTO;
import com.temipora.backend_springboot.modelo.ActualizarTelefonoDTO;
import com.temipora.backend_springboot.modelo.BajaCuentaResultadoDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.QuitarTelefonoDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.servicios.BajaCuentaService;
import com.temipora.backend_springboot.servicios.UsuarioService;
import com.temipora.backend_springboot.servicios.VisitaService;

@WebMvcTest(controllers = UsuarioController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class UsuarioControllerTest {

    private static final String ADMIN = "CosmicCrap";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private VisitaService visitaService;

    @MockitoBean
    private BajaCuentaService bajaCuentaService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ---------------------------------------------------------------
    // GET /api/usuarios
    // ---------------------------------------------------------------

    @Test
    void obtenerUsuarios_sinToken_devuelve401YNoLlamaAlServicio() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "idUsuario,asc"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("No autenticado."));

        verify(usuarioService, never())
                .obtenerUsuariosPaginados(any(), any(), any(), any(), any());
    }

    @Test
    void obtenerUsuarios_tokenBasura_devuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                .header("Authorization", "Bearer token-basura"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No autenticado."));
    }

    @Test
    void obtenerUsuarios_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("cliente42", ERol.CLIENTE.name());

        mockMvc.perform(get("/api/usuarios")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verify(usuarioService, never())
                .obtenerUsuariosPaginados(any(), any(), any(), any(), any());
    }

    @Test
    void obtenerUsuarios_tokenDeAdmin_devuelve200ConPaginaYPageableEsperado() throws Exception {
        stubUsuarioConRol(ADMIN, ERol.ADMIN);
        stubPaginaDeUsuarios();
        String token = jwtUtils.generarTokenAcceso(ADMIN, ERol.ADMIN.name());

        mockMvc.perform(get("/api/usuarios")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "10")
                .param("sort", "idUsuario,asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].nombreUsuario").value("Artista777"))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioService).obtenerUsuariosPaginados(
                isNull(), isNull(), isNull(), isNull(), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).containsExactly(Sort.Order.asc("idUsuario"));
    }

    @Test
    void obtenerUsuarios_conFiltros_pasaLosParametrosAlServicio() throws Exception {
        stubUsuarioConRol(ADMIN, ERol.ADMIN);
        stubPaginaDeUsuarios();
        String token = jwtUtils.generarTokenAcceso(ADMIN, ERol.ADMIN.name());

        mockMvc.perform(get("/api/usuarios")
                .header("Authorization", "Bearer " + token)
                .param("nombreUsuario", "art")
                .param("correo", "gmail")
                .param("estado", "true")
                .param("rol", "ARTISTA")
                .param("page", "1")
                .param("size", "5")
                .param("sort", "idUsuario,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioService).obtenerUsuariosPaginados(
                eq("art"), eq("gmail"), eq(true), eq(ERol.ARTISTA), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
    }

    @Test
    void obtenerUsuarios_conRolInvalido_devuelve400SinLlamarAlServicio() throws Exception {
        stubUsuarioConRol(ADMIN, ERol.ADMIN);
        String token = jwtUtils.generarTokenAcceso(ADMIN, ERol.ADMIN.name());

        mockMvc.perform(get("/api/usuarios")
                .header("Authorization", "Bearer " + token)
                .param("rol", "PEPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parámetro inválido: rol"));

        verify(usuarioService, never())
                .obtenerUsuariosPaginados(any(), any(), any(), any(), any());
    }

    // ---------------------------------------------------------------
    // PUT /api/usuarios/{idUsuario}/perfil
    // ---------------------------------------------------------------

    @Test
    void actualizarPerfil_sinToken_devuelve401YNoLlamaAlServicio() throws Exception {
        mockMvc.perform(put("/api/usuarios/7/perfil")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreDisplay\":\"Nuevo\"}"))
                .andExpect(status().isUnauthorized());

        verify(usuarioService, never()).actualizarPerfil(any(), any());
    }

    @Test
    void actualizarPerfil_conTokenValido_devuelve200YActualizaElPerfil() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("cliente42", ERol.CLIENTE.name());
        UsuarioDTO actualizado = UsuarioDTO.builder()
                .idUsuario(7L)
                .nombreDisplay("Nuevo")
                .build();
        when(usuarioService.nombreUsuario(7L)).thenReturn("cliente42");
        when(usuarioService.actualizarPerfil(eq(7L), any(ActualizarPerfilDTO.class)))
                .thenReturn(actualizado);

        mockMvc.perform(put("/api/usuarios/7/perfil")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreDisplay\":\"Nuevo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(7))
                .andExpect(jsonPath("$.nombreDisplay").value("Nuevo"));

        ArgumentCaptor<ActualizarPerfilDTO> dtoCaptor = ArgumentCaptor
                .forClass(ActualizarPerfilDTO.class);
        verify(usuarioService).actualizarPerfil(eq(7L), dtoCaptor.capture());
        assertThat(dtoCaptor.getValue().getNombreDisplay()).isEqualTo("Nuevo");
    }

    // ---------------------------------------------------------------
    // PUT /api/usuarios/{idUsuario}/correo
    // ---------------------------------------------------------------

    @Test
    void actualizarPerfil_tokenDeOtroUsuario_devuelve403YNoActualiza() throws Exception {
        stubUsuarioConRol("intruso", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("intruso", ERol.CLIENTE.name());
        when(usuarioService.nombreUsuario(7L)).thenReturn("CosmicCrap");

        mockMvc.perform(put("/api/usuarios/7/perfil")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombreDisplay\":\"Hackeado\"}"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).actualizarPerfil(any(), any());
    }

    @Test
    void cambiarCorreo_sinToken_devuelve401YNoLlamaAlServicio() throws Exception {
        mockMvc.perform(put("/api/usuarios/7/correo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"nuevo@mail.com\",\"contraseniaActual\":\"secreta123\"}"))
                .andExpect(status().isUnauthorized());

        verify(usuarioService, never()).cambiarCorreo(any(), any());
    }

    @Test
    void cambiarCorreo_tokenValido_devuelve200YActualiza() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("cliente42", ERol.CLIENTE.name());
        when(usuarioService.nombreUsuario(7L)).thenReturn("cliente42");
        when(usuarioService.cambiarCorreo(eq(7L), any(ActualizarCorreoDTO.class)))
                .thenReturn(UsuarioDTO.builder().idUsuario(7L).correo("nuevo@mail.com").build());

        mockMvc.perform(put("/api/usuarios/7/correo")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"nuevo@mail.com\",\"contraseniaActual\":\"secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("nuevo@mail.com"));
    }

    @Test
    void cambiarCorreo_tokenDeOtroUsuario_devuelve403() throws Exception {
        stubUsuarioConRol("intruso", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("intruso", ERol.CLIENTE.name());
        when(usuarioService.nombreUsuario(7L)).thenReturn("CosmicCrap");

        mockMvc.perform(put("/api/usuarios/7/correo")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"nuevo@mail.com\",\"contraseniaActual\":\"secreta123\"}"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).cambiarCorreo(any(), any());
    }

    // ---------------------------------------------------------------
    // PUT /api/usuarios/{idUsuario}/telefono
    // ---------------------------------------------------------------

    @Test
    void cambiarTelefono_sinToken_devuelve401YNoLlamaAlServicio() throws Exception {
        mockMvc.perform(put("/api/usuarios/7/telefono")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefonoActual\":\"+16502530000\",\"telefonoNuevo\":\"+5491123456789\",\"contraseniaActual\":\"secreta123\"}"))
                .andExpect(status().isUnauthorized());

        verify(usuarioService, never()).cambiarTelefono(any(), any());
    }

    @Test
    void cambiarTelefono_tokenValido_devuelve200ConTelefonoEnmascarado() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("cliente42", ERol.CLIENTE.name());
        when(usuarioService.nombreUsuario(7L)).thenReturn("cliente42");
        when(usuarioService.cambiarTelefono(eq(7L), any(ActualizarTelefonoDTO.class)))
                .thenReturn(UsuarioDTO.builder()
                        .idUsuario(7L)
                        .tieneTelefono(true)
                        .telefonoEnmascarado("+54 *******6789")
                        .build());

        mockMvc.perform(put("/api/usuarios/7/telefono")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefonoActual\":\"+16502530000\",\"telefonoNuevo\":\"+5491123456789\",\"contraseniaActual\":\"secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneTelefono").value(true))
                .andExpect(jsonPath("$.telefonoEnmascarado").value("+54 *******6789"))
                .andExpect(jsonPath("$.telefono").doesNotExist());

        verify(usuarioService).cambiarTelefono(eq(7L), any(ActualizarTelefonoDTO.class));
    }

    @Test
    void cambiarTelefono_tokenDeOtroUsuario_devuelve403() throws Exception {
        stubUsuarioConRol("intruso", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("intruso", ERol.CLIENTE.name());
        when(usuarioService.nombreUsuario(7L)).thenReturn("CosmicCrap");

        mockMvc.perform(put("/api/usuarios/7/telefono")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefonoNuevo\":\"+5491123456789\",\"contraseniaActual\":\"secreta123\"}"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).cambiarTelefono(any(), any());
    }

    @Test
    void cambiarTelefono_sinTelefonoNuevo_devuelve400SinLlamarAlServicio() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("cliente42", ERol.CLIENTE.name());

        mockMvc.perform(put("/api/usuarios/7/telefono")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contraseniaActual\":\"secreta123\"}"))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).cambiarTelefono(any(), any());
    }

    // ---------------------------------------------------------------
    // DELETE /api/usuarios/{idUsuario}/telefono
    // ---------------------------------------------------------------

    @Test
    void quitarTelefono_sinToken_devuelve401YNoLlamaAlServicio() throws Exception {
        mockMvc.perform(delete("/api/usuarios/7/telefono")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contraseniaActual\":\"secreta123\"}"))
                .andExpect(status().isUnauthorized());

        verify(usuarioService, never()).quitarTelefono(any(), any());
    }

    @Test
    void quitarTelefono_tokenValido_devuelve200SinTelefono() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("cliente42", ERol.CLIENTE.name());
        when(usuarioService.nombreUsuario(7L)).thenReturn("cliente42");
        when(usuarioService.quitarTelefono(eq(7L), any(QuitarTelefonoDTO.class)))
                .thenReturn(UsuarioDTO.builder()
                        .idUsuario(7L)
                        .tieneTelefono(false)
                        .telefonoEnmascarado(null)
                        .build());

        mockMvc.perform(delete("/api/usuarios/7/telefono")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contraseniaActual\":\"secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneTelefono").value(false))
                .andExpect(jsonPath("$.telefonoEnmascarado").doesNotExist());

        verify(usuarioService).quitarTelefono(eq(7L), any(QuitarTelefonoDTO.class));
    }

    @Test
    void quitarTelefono_tokenDeOtroUsuario_devuelve403() throws Exception {
        stubUsuarioConRol("intruso", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("intruso", ERol.CLIENTE.name());
        when(usuarioService.nombreUsuario(7L)).thenReturn("CosmicCrap");

        mockMvc.perform(delete("/api/usuarios/7/telefono")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contraseniaActual\":\"secreta123\"}"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).quitarTelefono(any(), any());
    }

    // ---------------------------------------------------------------
    // PUT /api/usuarios/{idUsuario}/contrasenia
    // ---------------------------------------------------------------

    @Test
    void cambiarContrasenia_sinToken_devuelve401YNoLlamaAlServicio() throws Exception {
        mockMvc.perform(put("/api/usuarios/7/contrasenia")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contraseniaActual\":\"Actual123\",\"contraseniaNueva\":\"Nueva123\",\"confirmarContrasenia\":\"Nueva123\"}"))
                .andExpect(status().isUnauthorized());

        verify(usuarioService, never()).cambiarContrasenia(any(), any());
    }

    @Test
    void cambiarContrasenia_tokenValido_devuelve200() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("cliente42", ERol.CLIENTE.name());
        when(usuarioService.nombreUsuario(7L)).thenReturn("cliente42");

        mockMvc.perform(put("/api/usuarios/7/contrasenia")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contraseniaActual\":\"Actual123\",\"contraseniaNueva\":\"Nueva123\",\"confirmarContrasenia\":\"Nueva123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contraseña actualizada correctamente."));

        verify(usuarioService).cambiarContrasenia(eq(7L), any());
    }

    @Test
    void cambiarContrasenia_tokenDeOtroUsuario_devuelve403() throws Exception {
        stubUsuarioConRol("intruso", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("intruso", ERol.CLIENTE.name());
        when(usuarioService.nombreUsuario(7L)).thenReturn("CosmicCrap");

        mockMvc.perform(put("/api/usuarios/7/contrasenia")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contraseniaActual\":\"Actual123\",\"contraseniaNueva\":\"Nueva123\",\"confirmarContrasenia\":\"Nueva123\"}"))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).cambiarContrasenia(any(), any());
    }

    @Test
    void cambiarContrasenia_contraseniaDebil_devuelve400SinLlamarAlServicio() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("cliente42", ERol.CLIENTE.name());

        mockMvc.perform(put("/api/usuarios/7/contrasenia")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contraseniaActual\":\"Actual123\",\"contraseniaNueva\":\"debil\",\"confirmarContrasenia\":\"debil\"}"))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).cambiarContrasenia(any(), any());
    }

    // ---------------------------------------------------------------
    // POST /api/usuarios/{idUsuario}/baja
    // ---------------------------------------------------------------

    @Test
    void darDeBaja_sinToken_devuelve401YNoLlamaAlServicio() throws Exception {
        mockMvc.perform(post("/api/usuarios/7/baja")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contrasenia\":\"secreta123\"}"))
                .andExpect(status().isUnauthorized());

        verify(bajaCuentaService, never()).darDeBaja(any(), any());
    }

    @Test
    void darDeBaja_tokenPropio_devuelve200YDelega() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("cliente42", ERol.CLIENTE.name());
        when(usuarioService.nombreUsuario(7L)).thenReturn("cliente42");
        when(bajaCuentaService.darDeBaja(7L, "secreta123"))
                .thenReturn(new BajaCuentaResultadoDTO(
                        "Tu cuenta quedó inactiva.",
                        LocalDateTime.now().plusDays(30)));

        mockMvc.perform(post("/api/usuarios/7/baja")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contrasenia\":\"secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tu cuenta quedó inactiva."))
                .andExpect(jsonPath("$.reactivarHasta").isNotEmpty());

        verify(bajaCuentaService).darDeBaja(7L, "secreta123");
    }

    @Test
    void darDeBaja_tokenDeOtroUsuario_devuelve403() throws Exception {
        stubUsuarioConRol("intruso", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("intruso", ERol.CLIENTE.name());
        when(usuarioService.nombreUsuario(7L)).thenReturn("CosmicCrap");

        mockMvc.perform(post("/api/usuarios/7/baja")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contrasenia\":\"secreta123\"}"))
                .andExpect(status().isForbidden());

        verify(bajaCuentaService, never()).darDeBaja(any(), any());
    }

    @Test
    void darDeBaja_sinContrasenia_devuelve400SinLlamarAlServicio() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        String token = jwtUtils.generarTokenAcceso("cliente42", ERol.CLIENTE.name());

        mockMvc.perform(post("/api/usuarios/7/baja")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verify(bajaCuentaService, never()).darDeBaja(any(), any());
    }

    // ---------------------------------------------------------------
    // GET /api/perfil/{nombreUsuario} — cuenta inactiva
    // ---------------------------------------------------------------

    @Test
    void obtenerPerfil_cuentaInactiva_devuelve410() throws Exception {
        when(usuarioService.getByNombreUsuario("baja"))
                .thenReturn(Usuario.builder()
                        .idUsuario(9L)
                        .nombreUsuario("baja")
                        .estadoCuenta(false)
                        .build());

        mockMvc.perform(get("/api/perfil/baja"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message")
                        .value("Esta cuenta no está disponible."));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void stubUsuarioConRol(String nombreUsuario, ERol rol) {
        when(userDetailsService.loadUserByUsername(nombreUsuario))
                .thenReturn(User.withUsername(nombreUsuario)
                        .password("{noop}prueba")
                        .authorities("ROLE_" + rol.name())
                        .build());
    }

    private void stubPaginaDeUsuarios() {
        UsuarioDTO usuario = UsuarioDTO.builder()
                .idUsuario(1L)
                .nombreUsuario("Artista777")
                .correo("artista@mail.com")
                .rol("ARTISTA")
                .build();
        when(usuarioService.obtenerUsuariosPaginados(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(usuario), PageRequest.of(0, 10), 1));
    }
}
