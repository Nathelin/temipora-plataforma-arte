package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    // GET /api/favoritos
    // ---------------------------------------------------------------

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/favoritos"))
            .andExpect(status().isUnauthorized());

        verify(favoritoService, never()).listar(any());
    }

    @Test
    void listar_conToken_devuelveFavoritos() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.listar("cliente1")).thenReturn(List.of(dto()));

        mockMvc.perform(get("/api/favoritos")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombreUsuario").value("artista1"))
            .andExpect(jsonPath("$[0].tipoArtePrincipal").value("Acuarela"))
            .andExpect(jsonPath("$[0].medioPrincipal").value("Digital"));
    }

    // ---------------------------------------------------------------
    // GET /api/favoritos/{nombreUsuario}
    // ---------------------------------------------------------------

    @Test
    void estado_conToken_devuelveSiEsFavorito() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.esFavorito("cliente1", "artista1")).thenReturn(true);

        mockMvc.perform(get("/api/favoritos/artista1")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.esFavorito").value(true));
    }

    // ---------------------------------------------------------------
    // POST /api/favoritos/{nombreUsuario}
    // ---------------------------------------------------------------

    @Test
    void agregar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/api/favoritos/artista1"))
            .andExpect(status().isUnauthorized());

        verify(favoritoService, never()).agregar(any(), any());
    }

    @Test
    void agregar_conToken_devuelveArtistaFavorito() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.agregar("cliente1", "artista1")).thenReturn(dto());

        mockMvc.perform(post("/api/favoritos/artista1")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombreUsuario").value("artista1"));
    }

    @Test
    void agregar_servicioRechazaOperacion_devuelve400() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.agregar("cliente1", "cliente2"))
            .thenThrow(new IllegalArgumentException("El usuario indicado no es un artista."));

        mockMvc.perform(post("/api/favoritos/cliente2")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("El usuario indicado no es un artista."));
    }

    // ---------------------------------------------------------------
    // DELETE /api/favoritos/{nombreUsuario}
    // ---------------------------------------------------------------

    @Test
    void eliminar_conToken_devuelveMensajeDeExito() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(favoritoService.eliminar("cliente1", "artista1")).thenReturn(true);

        mockMvc.perform(delete("/api/favoritos/artista1")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.esFavorito").value(false))
            .andExpect(jsonPath("$.message").value("El artista se eliminó de tus favoritos."));
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
}
