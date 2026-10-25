package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.config.SecurityConfig;
import com.temipora.backend_springboot.modelo.ArtistaFavoritoDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.ServicioFavoritoDTO;
import com.temipora.backend_springboot.servicios.FavoritoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = FavoritoController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class FavoritoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private FavoritoService favoritoService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ---------------------------------------------------------------
    // GET /api/favoritos/artistas
    // ---------------------------------------------------------------

    @Test
    void listarArtistas_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/favoritos/artistas"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(favoritoService);
    }

    @Test
    void listarArtistas_conToken_devuelveFavoritos() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.listarArtistas("cliente1", 0, 12, null, "recientes"))
            .thenReturn(pagina(List.of(dto()), 1));

        mockMvc.perform(get("/api/favoritos/artistas")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido[0].nombreUsuario").value("artista1"))
            .andExpect(jsonPath("$.contenido[0].tipoArtePrincipal").value("Acuarela"))
            .andExpect(jsonPath("$.contenido[0].medioPrincipal").value("Digital"))
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.hayMas").value(false));
    }

    @Test
    void listarArtistas_conParametros_losPropagaAlServicio() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.listarArtistas("cliente1", 1, 6, "retrato", "alfabetico"))
            .thenReturn(pagina(List.of(), 0));

        mockMvc.perform(get("/api/favoritos/artistas")
                .param("pagina", "1")
                .param("tamanio", "6")
                .param("q", "retrato")
                .param("orden", "alfabetico")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk());

        verify(favoritoService).listarArtistas("cliente1", 1, 6, "retrato", "alfabetico");
    }

    // ---------------------------------------------------------------
    // GET /api/favoritos/artistas/{nombreUsuario}
    // ---------------------------------------------------------------

    @Test
    void estadoArtista_conToken_devuelveSiEsFavorito() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.esFavoritoArtista("cliente1", "artista1")).thenReturn(true);

        mockMvc.perform(get("/api/favoritos/artistas/artista1")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.esFavorito").value(true));
    }

    // ---------------------------------------------------------------
    // POST /api/favoritos/artistas/{nombreUsuario}
    // ---------------------------------------------------------------

    @Test
    void agregarArtista_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/api/favoritos/artistas/artista1"))
            .andExpect(status().isUnauthorized());

        verify(favoritoService, never()).agregarArtista(any(), any());
    }

    @Test
    void agregarArtista_conToken_devuelveArtistaFavorito() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.agregarArtista("cliente1", "artista1")).thenReturn(dto());

        mockMvc.perform(post("/api/favoritos/artistas/artista1")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombreUsuario").value("artista1"));
    }

    @Test
    void agregarArtista_servicioRechazaOperacion_devuelve400() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.agregarArtista("cliente1", "cliente2"))
            .thenThrow(new IllegalArgumentException("El usuario indicado no es un artista."));

        mockMvc.perform(post("/api/favoritos/artistas/cliente2")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("El usuario indicado no es un artista."));
    }

    // ---------------------------------------------------------------
    // DELETE /api/favoritos/artistas/{nombreUsuario}
    // ---------------------------------------------------------------

    @Test
    void eliminarArtista_conToken_devuelveMensajeDeExito() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.eliminarArtista("cliente1", "artista1")).thenReturn(true);

        mockMvc.perform(delete("/api/favoritos/artistas/artista1")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.esFavorito").value(false))
            .andExpect(jsonPath("$.message").value("El artista se eliminó de tus favoritos."));
    }

    // ---------------------------------------------------------------
    // GET /api/favoritos/servicios
    // ---------------------------------------------------------------

    @Test
    void listarServicios_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/favoritos/servicios"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(favoritoService);
    }

    @Test
    void listarServicios_conToken_devuelveFavoritos() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.listarServicios("cliente1", 0, 12, null, "recientes", "todos"))
            .thenReturn(pagina(List.of(servicioDto()), 1));

        mockMvc.perform(get("/api/favoritos/servicios")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido[0].idServicio").value(5))
            .andExpect(jsonPath("$.contenido[0].disponible").value(true))
            .andExpect(jsonPath("$.contenido[0].servicio.nombre").value("Retrato digital"))
            .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void listarServicios_conParametros_losPropagaAlServicio() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.listarServicios(
                "cliente1", 2, 6, "retrato", "alfabetico", "disponibles"))
            .thenReturn(pagina(List.of(), 0));

        mockMvc.perform(get("/api/favoritos/servicios")
                .param("pagina", "2")
                .param("tamanio", "6")
                .param("q", "retrato")
                .param("orden", "alfabetico")
                .param("estado", "disponibles")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk());

        verify(favoritoService).listarServicios(
            "cliente1", 2, 6, "retrato", "alfabetico", "disponibles");
    }

    @Test
    void listarIdsServicios_conToken_devuelveIds() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.listarIdsServicios("cliente1")).thenReturn(List.of(5L));

        mockMvc.perform(get("/api/favoritos/servicios/ids")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value(5));
    }

    // ---------------------------------------------------------------
    // GET /api/favoritos/servicios/{idServicio}
    // ---------------------------------------------------------------

    @Test
    void estadoServicio_conToken_devuelveSiEsFavorito() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.esFavoritoServicio("cliente1", 5L)).thenReturn(true);

        mockMvc.perform(get("/api/favoritos/servicios/5")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.esFavorito").value(true));
    }

    // ---------------------------------------------------------------
    // POST /api/favoritos/servicios/{idServicio}
    // ---------------------------------------------------------------

    @Test
    void agregarServicio_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/api/favoritos/servicios/5"))
            .andExpect(status().isUnauthorized());

        verify(favoritoService, never()).agregarServicio(any(), any());
    }

    @Test
    void agregarServicio_conToken_devuelveServicioFavorito() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.agregarServicio("cliente1", 5L)).thenReturn(servicioDto());

        mockMvc.perform(post("/api/favoritos/servicios/5")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idServicio").value(5))
            .andExpect(jsonPath("$.disponible").value(true));
    }

    // ---------------------------------------------------------------
    // DELETE /api/favoritos/servicios/{idServicio}
    // ---------------------------------------------------------------

    @Test
    void eliminarServicio_conToken_devuelveMensajeDeExito() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.eliminarServicio("cliente1", 5L)).thenReturn(true);

        mockMvc.perform(delete("/api/favoritos/servicios/5")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.esFavorito").value(false))
            .andExpect(jsonPath("$.message").value("El servicio se eliminó de tus favoritos."));
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

    private String token(String nombreUsuario, ERol rol) {
        return jwtUtils.generarTokenAcceso(nombreUsuario, rol.name());
    }

    private ArtistaFavoritoDTO dto() {
        return ArtistaFavoritoDTO.builder()
            .idUsuario(2L)
            .nombreUsuario("artista1")
            .nombreDisplay("Artista Uno")
            .tipoArtePrincipal("Acuarela")
            .medioPrincipal("Digital")
            .etiquetasDestacadas(List.of("Retrato"))
            .tiposServicioDestacados(List.of("Comisión"))
            .fechaAgregado(LocalDateTime.now())
            .cantidadImagenes(3)
            .cantidadServicios(1)
            .build();
    }

    private ServicioFavoritoDTO servicioDto() {
        return ServicioFavoritoDTO.builder()
            .idServicio(5L)
            .fechaAgregado(LocalDateTime.now())
            .disponible(true)
            .servicio(ServicioDTO.builder()
                .idServicio(5L)
                .nombre("Retrato digital")
                .nombreArtista("artista1")
                .build())
            .build();
    }

    private <T> PaginaDTO<T> pagina(List<T> contenido, long total) {
        return PaginaDTO.<T>builder()
            .contenido(contenido)
            .pagina(0)
            .tamanio(12)
            .totalElementos(total)
            .totalPaginas(total == 0 ? 0 : 1)
            .hayMas(false)
            .build();
    }
}
