package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.temipora.backend_springboot.modelo.ActualizarPerfilDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.servicios.UsuarioService;

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
