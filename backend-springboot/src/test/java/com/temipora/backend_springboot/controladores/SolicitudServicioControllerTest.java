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
import com.temipora.backend_springboot.modelo.SolicitudDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.SolicitudServicioService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = SolicitudServicioController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class SolicitudServicioControllerTest {

    private static final String JSON_VALIDO = "{\"idServicio\":1,\"idTerminos\":2,\"terminosVersion\":1," +
            "\"aceptaTerminos\":true,\"items\":[{\"nombre\":\"Dibujo 1\",\"respuestas\":[]}]}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private SolicitudServicioService solicitudServicioService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void crear_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/api/solicitudes-servicio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isUnauthorized());

        verify(solicitudServicioService, never()).crear(any(), any());
    }

    @Test
    void crear_tokenDeCliente_devuelve201() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(usuarioService.getByNombreUsuario("cliente1")).thenReturn(usuario(5L));
        when(solicitudServicioService.crear(eq(5L), any())).thenReturn(dto());

        mockMvc.perform(post("/api/solicitudes-servicio")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idSolicitud").value(1));
    }

    @Test
    void crear_tokenDeArtista_devuelve201() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(usuarioService.getByNombreUsuario("artista1")).thenReturn(usuario(7L));
        when(solicitudServicioService.crear(eq(7L), any())).thenReturn(dto());

        mockMvc.perform(post("/api/solicitudes-servicio")
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idSolicitud").value(1));
    }

    @Test
    void crear_tokenDeAdmin_devuelve403() throws Exception {
        stubUsuarioConRol("admin1", ERol.ADMIN);

        mockMvc.perform(post("/api/solicitudes-servicio")
                .header("Authorization", "Bearer " + token("admin1", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isForbidden());

        verify(solicitudServicioService, never()).crear(any(), any());
    }

    @Test
    void enviadas_tokenDeCliente_devuelve200() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);
        when(usuarioService.getByNombreUsuario("cliente1")).thenReturn(usuario(5L));
        when(solicitudServicioService.listarEnviadas(5L, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/solicitudes-servicio/enviadas")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
                .andExpect(status().isOk());
    }

    @Test
    void enviadas_tokenDeArtista_devuelve200() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(usuarioService.getByNombreUsuario("artista1")).thenReturn(usuario(7L));
        when(solicitudServicioService.listarEnviadas(7L, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/solicitudes-servicio/enviadas")
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA)))
                .andExpect(status().isOk());
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

    private Usuario usuario(Long id) {
        return Usuario.builder().idUsuario(id).nombreUsuario("usuario" + id).build();
    }

    private SolicitudDTO dto() {
        return SolicitudDTO.builder()
                .idSolicitud(1L)
                .nombreServicio("Ilustración")
                .build();
    }
}
