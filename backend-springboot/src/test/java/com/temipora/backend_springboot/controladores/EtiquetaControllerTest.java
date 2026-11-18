package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.temipora.backend_springboot.modelo.CategoriaEtiquetaDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.EtiquetaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = EtiquetaController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class EtiquetaControllerTest {

    private static final String BASE = "/api/etiquetas";

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

    private final Usuario artista = usuario(20L, "dibujito");

    @Test
    void selector_sinToken_devuelve200() throws Exception {
        // El catálogo de etiquetas es público: el filtro del catálogo de
        // servicios lo necesita para un visitante anónimo.
        when(etiquetaService.selector(null))
                .thenReturn(List.of(dto(1L, "Acuarela", EtiquetaEstado.VALIDA)));

        mockMvc.perform(get(BASE + "/selector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Acuarela"));
    }

    @Test
    void selector_tokenDeArtista_devuelve200() throws Exception {
        stubUsuarioConRol("dibujito", ERol.ARTISTA);
        stubUsuarioActual(artista);
        when(etiquetaService.selector(artista))
                .thenReturn(List.of(dto(1L, "Acuarela", EtiquetaEstado.VALIDA)));

        mockMvc.perform(get(BASE + "/selector")
                .header("Authorization", "Bearer " + token("dibujito", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Acuarela"));
    }

    @Test
    void categorias_sinToken_devuelve200() throws Exception {
        when(etiquetaService.categoriasActivas())
                .thenReturn(List.of(CategoriaEtiquetaDTO.builder()
                        .idCategoriaEtiqueta(1L).nombre("Estilo").activo(true).build()));

        mockMvc.perform(get(BASE + "/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Estilo"));
    }

    @Test
    void categorias_conToken_devuelve200() throws Exception {
        stubUsuarioConRol("dibujito", ERol.ARTISTA);
        when(etiquetaService.categoriasActivas())
                .thenReturn(List.of(CategoriaEtiquetaDTO.builder()
                        .idCategoriaEtiqueta(1L).nombre("Estilo").activo(true).build()));

        mockMvc.perform(get(BASE + "/categorias")
                .header("Authorization", "Bearer " + token("dibujito", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Estilo"));
    }

    @Test
    void buscar_encontrada_devuelve200() throws Exception {
        stubUsuarioConRol("dibujito", ERol.ARTISTA);
        when(etiquetaService.buscar("Acuarela"))
                .thenReturn(Optional.of(dto(1L, "Acuarela", EtiquetaEstado.VALIDA)));

        mockMvc.perform(get(BASE + "/buscar")
                .param("nombre", "Acuarela")
                .header("Authorization", "Bearer " + token("dibujito", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Acuarela"));
    }

    @Test
    void buscar_noEncontrada_devuelve404() throws Exception {
        stubUsuarioConRol("dibujito", ERol.ARTISTA);
        when(etiquetaService.buscar("fantasma")).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/buscar")
                .param("nombre", "fantasma")
                .header("Authorization", "Bearer " + token("dibujito", ERol.ARTISTA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void misEtiquetas_tokenDeArtista_devuelve200() throws Exception {
        stubUsuarioConRol("dibujito", ERol.ARTISTA);
        stubUsuarioActual(artista);
        when(etiquetaService.misEtiquetas(artista))
                .thenReturn(List.of(dto(1L, "Pixel", EtiquetaEstado.PENDIENTE)));

        mockMvc.perform(get(BASE + "/mis")
                .header("Authorization", "Bearer " + token("dibujito", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    @Test
    void crear_tokenDeArtista_devuelve201() throws Exception {
        stubUsuarioConRol("dibujito", ERol.ARTISTA);
        stubUsuarioActual(artista);
        when(etiquetaService.crearDesdeArtista(eq(artista), any()))
                .thenReturn(dto(1L, "Pixel", EtiquetaEstado.PENDIENTE));

        mockMvc.perform(post(BASE)
                .header("Authorization", "Bearer " + token("dibujito", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Pixel\",\"descripcion\":\"Estilo pixel art\","
                        + "\"idCategoria\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void crear_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(post(BASE)
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Pixel\",\"descripcion\":\"Estilo pixel art\","
                        + "\"idCategoria\":1}"))
                .andExpect(status().isForbidden());

        verify(etiquetaService, never()).crearDesdeArtista(any(), any());
    }

    @Test
    void reSolicitar_tokenDeArtista_devuelve200() throws Exception {
        stubUsuarioConRol("dibujito", ERol.ARTISTA);
        stubUsuarioActual(artista);
        when(etiquetaService.reSolicitar(artista, 1L))
                .thenReturn(dto(1L, "Manga", EtiquetaEstado.RECHAZADA));

        mockMvc.perform(post(BASE + "/1/re-solicitar")
                .header("Authorization", "Bearer " + token("dibujito", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));
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
    }

    private String token(String nombreUsuario, ERol rol) {
        return jwtUtils.generarTokenAcceso(nombreUsuario, rol.name());
    }

    private EtiquetaDTO dto(Long id, String nombre, EtiquetaEstado estado) {
        return EtiquetaDTO.builder()
                .idEtiqueta(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .idCategoria(1L)
                .nombreCategoria("Estilo")
                .estado(estado)
                .vecesSolicitada(1)
                .creadoPor("dibujito")
                .build();
    }

    private Usuario usuario(Long id, String nombreUsuario) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombreUsuario);
        return usuario;
    }
}
