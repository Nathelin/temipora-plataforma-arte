package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.temipora.backend_springboot.modelo.AmbitoResenia;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EtiquetaReputacion;
import com.temipora.backend_springboot.modelo.ReputacionClienteDTO;
import com.temipora.backend_springboot.modelo.ReseniaDTO;
import com.temipora.backend_springboot.modelo.ReseniaEstadoDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.ReseniaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = ReseniaController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class ReseniaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private ReseniaService reseniaService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void estado_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/pedidos/100/resenia"))
                .andExpect(status().isUnauthorized());

        verify(reseniaService, never()).estado(any(), any());
    }

    @Test
    void estado_tokenDeAdmin_devuelve403() throws Exception {
        stubAutenticado("admin", ERol.ADMIN);

        mockMvc.perform(get("/api/pedidos/100/resenia")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isForbidden());

        verify(reseniaService, never()).estado(any(), any());
    }

    @Test
    void estado_tokenDeCliente_devuelve200() throws Exception {
        stubAutenticado("cliente42", ERol.CLIENTE);
        when(reseniaService.estado(100L, 10L)).thenReturn(estadoDto());

        mockMvc.perform(get("/api/pedidos/100/resenia")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion").value("SERVICIO"))
                .andExpect(jsonPath("$.puedeCrear").value(true));
    }

    @Test
    void crear_tokenDeCliente_devuelve201() throws Exception {
        stubAutenticado("cliente42", ERol.CLIENTE);
        when(reseniaService.crear(eq(100L), eq(10L), any())).thenReturn(reseniaDto());

        mockMvc.perform(post("/api/pedidos/100/resenia")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"descripcion":"Llegó en fecha",
                     "calificacionGeneral":5,"valores":[],"opciones":[]}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idResenia").value(1));
    }

    @Test
    void crear_sinCalificacion_devuelve400() throws Exception {
        stubAutenticado("cliente42", ERol.CLIENTE);

        mockMvc.perform(post("/api/pedidos/100/resenia")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"descripcion":"Llegó en fecha"}
                    """))
                .andExpect(status().isBadRequest());

        verify(reseniaService, never()).crear(any(), any(), any());
    }

    @Test
    void eliminar_tokenDeCliente_devuelve204() throws Exception {
        stubAutenticado("cliente42", ERol.CLIENTE);

        mockMvc.perform(delete("/api/pedidos/100/resenia")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isNoContent());

        verify(reseniaService).eliminar(100L, 10L);
    }

    @Test
    void reputacion_tokenDeCliente_devuelve403() throws Exception {
        stubAutenticado("cliente42", ERol.CLIENTE);

        mockMvc.perform(get("/api/clientes/30/reputacion")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isForbidden());

        verify(reseniaService, never()).reputacionCliente(any(), any());
    }

    @Test
    void reputacion_tokenDeArtista_devuelve200() throws Exception {
        stubAutenticado("artista42", ERol.ARTISTA);
        when(reseniaService.reputacionCliente(30L, 20L))
                .thenReturn(ReputacionClienteDTO.builder()
                        .idCliente(30L)
                        .cantidadResenias(4)
                        .etiqueta(EtiquetaReputacion.EXCELENTE)
                        .hayDatos(true)
                        .minimoResenias(3)
                        .build());

        mockMvc.perform(get("/api/clientes/30/reputacion")
                .header("Authorization", "Bearer " + token("artista42", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etiqueta").value("EXCELENTE"))
                .andExpect(jsonPath("$.cantidadResenias").value(4));
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void stubAutenticado(String nombreUsuario, ERol rol) {
        Long id = rol == ERol.ARTISTA ? 20L : 10L;
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombreUsuario);

        when(userDetailsService.loadUserByUsername(nombreUsuario))
                .thenReturn(User.withUsername(nombreUsuario)
                        .password("{noop}prueba")
                        .authorities("ROLE_" + rol.name())
                        .build());
        when(usuarioService.getByNombreUsuario(nombreUsuario)).thenReturn(usuario);
    }

    private String token(String nombreUsuario, ERol rol) {
        return jwtUtils.generarTokenAcceso(nombreUsuario, rol.name());
    }

    private ReseniaEstadoDTO estadoDto() {
        return ReseniaEstadoDTO.builder()
                .direccion(AmbitoResenia.SERVICIO)
                .puedeCrear(true)
                .puedeEditar(false)
                .puedeEliminar(false)
                .build();
    }

    private ReseniaDTO reseniaDto() {
        return ReseniaDTO.builder()
                .idResenia(1L)
                .idPedido(100L)
                .direccion(AmbitoResenia.SERVICIO)
                .calificacionGeneral(5)
                .descripcion("Llegó en fecha")
                .build();
    }
}
