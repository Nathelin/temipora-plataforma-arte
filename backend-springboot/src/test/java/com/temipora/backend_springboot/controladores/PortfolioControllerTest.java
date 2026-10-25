package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import com.temipora.backend_springboot.modelo.FiltroPortfolioRequest;
import com.temipora.backend_springboot.modelo.ImagenPortfolioDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.PortfolioService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = PortfolioController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private PortfolioService portfolioService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ---------------------------------------------------------------
    // GET /api/portfolio/{nombreUsuario}
    // ---------------------------------------------------------------

    @Test
    void listar_sinToken_devuelve200ConImagenes() throws Exception {
        when(portfolioService.listar("artista777"))
                .thenReturn(List.of(imagenDe("Obra pública")));

        mockMvc.perform(get("/api/portfolio/artista777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Obra pública"));

        verify(portfolioService).listar("artista777");
    }

    @Test
    void listar_conTokenValido_devuelve200ConImagenes() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        when(portfolioService.listar("artista777"))
                .thenReturn(List.of(imagenDe("Obra 1")));

        mockMvc.perform(get("/api/portfolio/artista777")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Obra 1"));
    }

    // ---------------------------------------------------------------
    // GET /api/portfolio/{nombreUsuario}/paginado
    // ---------------------------------------------------------------

    @Test
    void listarPaginado_visitanteResuelto_pasaSuIdComoViewer() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente42"))
                .thenReturn(Optional.of(Usuario.builder().idUsuario(55L).build()));
        when(portfolioService.listarParaVisorPorNombreUsuario(
                eq("artista777"), eq(55L), any(FiltroPortfolioRequest.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(imagenDe("Obra 1")), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/portfolio/artista777/paginado")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listarPaginado_visitanteNoRegistrado_pasaViewerNulo() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente42"))
                .thenReturn(Optional.empty());
        when(portfolioService.listarParaVisorPorNombreUsuario(
                eq("artista777"), isNull(), any(FiltroPortfolioRequest.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/portfolio/artista777/paginado")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listarPaginado_conFiltros_losPasaAlServicio() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente42"))
                .thenReturn(Optional.of(Usuario.builder().idUsuario(55L).build()));
        when(portfolioService.listarParaVisorPorNombreUsuario(
                eq("artista777"), eq(55L), any(FiltroPortfolioRequest.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/portfolio/artista777/paginado")
                .param("titulo", "obra")
                .param("idEtiqueta", "3")
                .param("idTipoArte", "2")
                .param("sensible", "true")
                .param("orden", "ANTIGUO")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isOk());

        ArgumentCaptor<FiltroPortfolioRequest> captor = ArgumentCaptor.forClass(FiltroPortfolioRequest.class);
        verify(portfolioService).listarParaVisorPorNombreUsuario(
                eq("artista777"), eq(55L), captor.capture(), any(Pageable.class));
        assertThat(captor.getValue().getTitulo()).isEqualTo("obra");
        assertThat(captor.getValue().getIdEtiqueta()).isEqualTo(3L);
        assertThat(captor.getValue().getIdTipoArte()).isEqualTo(2L);
        assertThat(captor.getValue().getSensible()).isEqualTo("true");
        assertThat(captor.getValue().getOrden()).isEqualTo("ANTIGUO");
    }

    @Test
    void listarPaginado_ordenInvalido_devuelve400() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente42"))
                .thenReturn(Optional.of(Usuario.builder().idUsuario(55L).build()));
        when(portfolioService.listarParaVisorPorNombreUsuario(
                eq("artista777"), eq(55L), any(FiltroPortfolioRequest.class), any(Pageable.class)))
                .thenThrow(new IllegalArgumentException("Orden no válido: ALEATORIO"));

        mockMvc.perform(get("/api/portfolio/artista777/paginado")
                .param("orden", "ALEATORIO")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Orden no válido: ALEATORIO"));
    }

    // ---------------------------------------------------------------
    // GET /api/portfolio/{nombreUsuario}/etiquetas
    // ---------------------------------------------------------------

    @Test
    void etiquetasPortfolio_sinToken_devuelve200ConEtiquetas() throws Exception {
        when(portfolioService.etiquetasDePortfolio("artista777"))
                .thenReturn(List.of(EtiquetaDTO.builder()
                        .idEtiqueta(1L)
                        .nombre("Ilustración")
                        .estado(EtiquetaEstado.VALIDA)
                        .build()));

        mockMvc.perform(get("/api/portfolio/artista777/etiquetas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Ilustración"));

        verify(portfolioService).etiquetasDePortfolio("artista777");
    }

    @Test
    void etiquetasPortfolio_conToken_devuelve200ConEtiquetas() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        when(portfolioService.etiquetasDePortfolio("artista777"))
                .thenReturn(List.of(EtiquetaDTO.builder()
                        .idEtiqueta(1L)
                        .nombre("Acuarela")
                        .estado(EtiquetaEstado.VALIDA)
                        .build()));

        mockMvc.perform(get("/api/portfolio/artista777/etiquetas")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Acuarela"));
    }

    // ---------------------------------------------------------------
    // POST /api/portfolio/usuarios/{idUsuario}/imagenes
    // ---------------------------------------------------------------

    @Test
    void crear_imagen_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/api/portfolio/usuarios/7/imagenes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestValido()))
                .andExpect(status().isUnauthorized());

        verify(portfolioService, never()).crear(any(), any());
    }

    @Test
    void crear_imagen_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(post("/api/portfolio/usuarios/7/imagenes")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestValido()))
                .andExpect(status().isForbidden());

        verify(portfolioService, never()).crear(any(), any());
    }

    @Test
    void crear_imagen_artistaDistintoAlDelToken_devuelve403() throws Exception {
        stubUsuarioConRol("artista777", ERol.ARTISTA);
        when(portfolioService.nombreUsuario(7L)).thenReturn("otroArtista");

        mockMvc.perform(post("/api/portfolio/usuarios/7/imagenes")
                .header("Authorization", "Bearer " + token("artista777", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestValido()))
                .andExpect(status().isForbidden());

        verify(portfolioService, never()).crear(any(), any());
    }

    @Test
    void crear_imagen_artistaCoincide_devuelve201() throws Exception {
        stubUsuarioConRol("artista777", ERol.ARTISTA);
        when(portfolioService.nombreUsuario(7L)).thenReturn("artista777");
        when(portfolioService.crear(eq(7L), any())).thenReturn(imagenDe("Mi obra"));

        mockMvc.perform(post("/api/portfolio/usuarios/7/imagenes")
                .header("Authorization", "Bearer " + token("artista777", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Mi obra"));
    }

    @Test
    void crear_imagen_bodyInvalido_devuelve400() throws Exception {
        stubUsuarioConRol("artista777", ERol.ARTISTA);

        mockMvc.perform(post("/api/portfolio/usuarios/7/imagenes")
                .header("Authorization", "Bearer " + token("artista777", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(portfolioService, never()).crear(any(), any());
    }

    // ---------------------------------------------------------------
    // PUT /api/portfolio/usuarios/{idUsuario}/imagenes/{idImagen}
    // ---------------------------------------------------------------

    @Test
    void modificar_imagen_sinToken_devuelve401() throws Exception {
        mockMvc.perform(put("/api/portfolio/usuarios/7/imagenes/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestValido()))
                .andExpect(status().isUnauthorized());

        verify(portfolioService, never()).modificar(any(), any(), any());
    }

    @Test
    void modificar_imagen_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(put("/api/portfolio/usuarios/7/imagenes/5")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestValido()))
                .andExpect(status().isForbidden());

        verify(portfolioService, never()).modificar(any(), any(), any());
    }

    @Test
    void modificar_imagen_artistaDistintoAlDelToken_devuelve403() throws Exception {
        stubUsuarioConRol("artista777", ERol.ARTISTA);
        when(portfolioService.nombreUsuario(7L)).thenReturn("otroArtista");

        mockMvc.perform(put("/api/portfolio/usuarios/7/imagenes/5")
                .header("Authorization", "Bearer " + token("artista777", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestValido()))
                .andExpect(status().isForbidden());

        verify(portfolioService, never()).modificar(any(), any(), any());
    }

    @Test
    void modificar_imagen_artistaCoincide_devuelve200() throws Exception {
        stubUsuarioConRol("artista777", ERol.ARTISTA);
        when(portfolioService.nombreUsuario(7L)).thenReturn("artista777");
        when(portfolioService.modificar(eq(7L), eq(5L), any())).thenReturn(imagenDe("Mi obra editada"));

        mockMvc.perform(put("/api/portfolio/usuarios/7/imagenes/5")
                .header("Authorization", "Bearer " + token("artista777", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestValido()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Mi obra editada"));

        verify(portfolioService).modificar(eq(7L), eq(5L), any());
    }

    @Test
    void modificar_imagen_inexistente_devuelve404() throws Exception {
        stubUsuarioConRol("artista777", ERol.ARTISTA);
        when(portfolioService.nombreUsuario(7L)).thenReturn("artista777");
        when(portfolioService.modificar(eq(7L), eq(99L), any()))
                .thenThrow(new NoSuchElementException("Imagen no encontrada: 99"));

        mockMvc.perform(put("/api/portfolio/usuarios/7/imagenes/99")
                .header("Authorization", "Bearer " + token("artista777", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestValido()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Imagen no encontrada: 99"));
    }

    @Test
    void modificar_imagen_bodyInvalido_devuelve400() throws Exception {
        stubUsuarioConRol("artista777", ERol.ARTISTA);

        mockMvc.perform(put("/api/portfolio/usuarios/7/imagenes/5")
                .header("Authorization", "Bearer " + token("artista777", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(portfolioService, never()).modificar(any(), any(), any());
    }

    // ---------------------------------------------------------------
    // POST /api/portfolio/usuarios/{idUsuario}/imagenes/{idImagen}/baja
    // ---------------------------------------------------------------

    @Test
    void baja_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/api/portfolio/usuarios/7/imagenes/5/baja"))
                .andExpect(status().isUnauthorized());

        verify(portfolioService, never()).darDeBaja(any(), any());
    }

    @Test
    void baja_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(post("/api/portfolio/usuarios/7/imagenes/5/baja")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isForbidden());

        verify(portfolioService, never()).darDeBaja(any(), any());
    }

    @Test
    void baja_artistaDistintoAlDelToken_devuelve403() throws Exception {
        stubUsuarioConRol("artista777", ERol.ARTISTA);
        when(portfolioService.nombreUsuario(7L)).thenReturn("otroArtista");

        mockMvc.perform(post("/api/portfolio/usuarios/7/imagenes/5/baja")
                .header("Authorization", "Bearer " + token("artista777", ERol.ARTISTA)))
                .andExpect(status().isForbidden());

        verify(portfolioService, never()).darDeBaja(any(), any());
    }

    @Test
    void baja_artistaCoincide_devuelve200() throws Exception {
        stubUsuarioConRol("artista777", ERol.ARTISTA);
        when(portfolioService.nombreUsuario(7L)).thenReturn("artista777");
        when(portfolioService.darDeBaja(7L, 5L)).thenReturn(imagenDe("Mi obra"));

        mockMvc.perform(post("/api/portfolio/usuarios/7/imagenes/5/baja")
                .header("Authorization", "Bearer " + token("artista777", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Mi obra"));

        verify(portfolioService).darDeBaja(7L, 5L);
    }

    @Test
    void baja_imagenNoEncontrada_devuelve404() throws Exception {
        stubUsuarioConRol("artista777", ERol.ARTISTA);
        when(portfolioService.nombreUsuario(7L)).thenReturn("artista777");
        when(portfolioService.darDeBaja(7L, 99L))
                .thenThrow(new NoSuchElementException("Imagen no encontrada: 99"));

        mockMvc.perform(post("/api/portfolio/usuarios/7/imagenes/99/baja")
                .header("Authorization", "Bearer " + token("artista777", ERol.ARTISTA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Imagen no encontrada: 99"));
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

    private String token(String nombreUsuario, ERol rol) {
        return jwtUtils.generarTokenAcceso(nombreUsuario, rol.name());
    }

    private ImagenPortfolioDTO imagenDe(String titulo) {
        return ImagenPortfolioDTO.builder()
                .idImagen(5L)
                .titulo(titulo)
                .url("https://x/img.png")
                .estado(true)
                .build();
    }

    private String requestValido() {
        return "{\"titulo\":\"Mi obra\",\"descripcion\":\"Una obra\","
                + "\"url\":\"https://x/img.png\",\"idTipoArte\":1}";
    }
}
