package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.config.SecurityConfig;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.EtiquetaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = EtiquetaAdminController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class EtiquetaAdminControllerTest {

    private static final String BASE = "/api/admin/etiquetas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private EtiquetaService etiquetaService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final Usuario adminUsuario = usuario(1L, "admin");

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(etiquetaService, never()).listarAdmin(any(), any());
    }

    @Test
    void listar_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isForbidden());

        verify(etiquetaService, never()).listarAdmin(any(), any());
    }

    @Test
    void listar_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(etiquetaService.listarAdmin(null, null))
                .thenReturn(List.of(dto(1L, "Acuarela", EtiquetaEstado.VALIDA, 0)));

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Acuarela"))
                .andExpect(jsonPath("$[0].estado").value("VALIDA"));
    }

    @Test
    void listar_conFiltroDeEstado_loPasaAlServicio() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(etiquetaService.listarAdmin(EtiquetaEstado.PENDIENTE, null)).thenReturn(List.of());

        mockMvc.perform(get(BASE)
                .param("estado", "PENDIENTE")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk());

        verify(etiquetaService).listarAdmin(EtiquetaEstado.PENDIENTE, null);
    }

    @Test
    void crear_tokenDeAdmin_devuelve201YResuelveAlAdmin() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        stubUsuarioActual(adminUsuario);
        when(etiquetaService.crearDesdeAdmin(eq(adminUsuario), any()))
                .thenReturn(dto(1L, "Acuarela", EtiquetaEstado.VALIDA, 0));

        mockMvc.perform(post(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Acuarela\",\"descripcion\":\"Tecnica\","
                        + "\"idCategoria\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("VALIDA"));

        verify(etiquetaService).crearDesdeAdmin(eq(adminUsuario), any());
    }

    @Test
    void crear_sinCategoria_devuelve400() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);

        mockMvc.perform(post(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Acuarela\",\"descripcion\":\"Tecnica\"}"))
                .andExpect(status().isBadRequest());

        verify(etiquetaService, never()).crearDesdeAdmin(any(), any());
    }

    @Test
    void crear_adminNoEncontradoEnRepositorio_devuelve404() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("admin")).thenReturn(Optional.empty());

        mockMvc.perform(post(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Acuarela\",\"descripcion\":\"Tecnica\","
                        + "\"idCategoria\":1}"))
                .andExpect(status().isNotFound());

        verify(etiquetaService, never()).crearDesdeAdmin(any(), any());
    }

    @Test
    void modificar_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(etiquetaService.modificar(eq(1L), any()))
                .thenReturn(dto(1L, "Acuarela", EtiquetaEstado.VALIDA, 0));

        mockMvc.perform(put(BASE + "/1")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Acuarela\",\"descripcion\":\"Tecnica\","
                        + "\"idCategoria\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Acuarela"));

        verify(etiquetaService).modificar(eq(1L), any());
    }

    @Test
    void aprobar_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        stubUsuarioActual(adminUsuario);
        when(etiquetaService.aprobar(adminUsuario, 1L))
                .thenReturn(dto(1L, "Pixel", EtiquetaEstado.VALIDA, 1));

        mockMvc.perform(post(BASE + "/1/aprobar")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDA"));

        verify(etiquetaService).aprobar(adminUsuario, 1L);
    }

    @Test
    void rechazar_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        stubUsuarioActual(adminUsuario);
        when(etiquetaService.rechazar(adminUsuario, 1L))
                .thenReturn(dto(1L, "Pixel", EtiquetaEstado.RECHAZADA, 1));

        mockMvc.perform(post(BASE + "/1/rechazar")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));

        verify(etiquetaService).rechazar(adminUsuario, 1L);
    }

    @Test
    void baja_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(etiquetaService.darDeBaja(1L))
                .thenReturn(dto(1L, "Acuarela", EtiquetaEstado.INACTIVA, 0));

        mockMvc.perform(post(BASE + "/1/baja")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVA"));
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void stubUsuarioConRol(String nombreUsuario, ERol rol) {
        when(userDetailsService.loadUserByUsername(nombreUsuario))
                .thenReturn(User.withUsername(nombreUsuario)
                        .password("{noop}prueba")
                        .authorities("ROLE_" + rol.name())
                        .build());
    }

    private void stubUsuarioActual(Usuario usuario) {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase(usuario.getNombreUsuario()))
                .thenReturn(Optional.of(usuario));
        assertThat(usuario.getIdUsuario()).isNotNull();
    }

    private String token(String nombreUsuario, ERol rol) {
        return jwtUtils.generarTokenAcceso(nombreUsuario, rol.name());
    }

    private EtiquetaDTO dto(Long id, String nombre, EtiquetaEstado estado, int demanda) {
        return EtiquetaDTO.builder()
                .idEtiqueta(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .idCategoria(1L)
                .nombreCategoria("Estilo")
                .estado(estado)
                .vecesSolicitada(demanda)
                .creadoPor("admin")
                .build();
    }

    private Usuario usuario(Long id, String nombreUsuario) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombreUsuario);
        return usuario;
    }
}
