package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.temipora.backend_springboot.modelo.TerminoTemaDTO;
import com.temipora.backend_springboot.modelo.TerminosDTO;
import com.temipora.backend_springboot.servicios.TerminosService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = TerminosController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class TerminosControllerTest {

    private static final String BASE = "/api/usuarios/7/terminos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private TerminosService terminosService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void obtener_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(terminosService, never()).obtenerGlobal(any());
    }

    @Test
    void obtener_tokenDeOtroUsuario_devuelve403() throws Exception {
        stubUsuarioConRol("intruso", ERol.ARTISTA);
        when(usuarioService.nombreUsuario(7L)).thenReturn("CosmicCrap");

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("intruso", ERol.ARTISTA)))
                .andExpect(status().isForbidden());

        verify(terminosService, never()).obtenerGlobal(any());
    }

    @Test
    void obtener_tokenValido_devuelve200() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(usuarioService.nombreUsuario(7L)).thenReturn("artista1");
        when(terminosService.obtenerGlobal(7L)).thenReturn(dto());

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.informacionGeneral").value("Hola"))
                .andExpect(jsonPath("$.temas[0].estado").value("SI"));
    }

    @Test
    void guardar_tokenValido_devuelve200() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(usuarioService.nombreUsuario(7L)).thenReturn("artista1");
        when(terminosService.guardarGlobal(eq(7L), any())).thenReturn(dto());

        mockMvc.perform(put(BASE)
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"informacionGeneral\":\"Hola\",\"temas\":[{\"idTemaTermino\":1,\"estado\":\"SI\"}]," +
                        "\"derechos\":[{\"idDerechoTermino\":1,\"disponible\":true,\"porcentajeExtra\":50}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.informacionGeneral").value("Hola"));
    }

    @Test
    void guardar_contenidoInvalido_devuelve400() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(usuarioService.nombreUsuario(7L)).thenReturn("artista1");
        when(terminosService.guardarGlobal(eq(7L), any()))
                .thenThrow(new IllegalArgumentException("Completá al menos una sección de tus términos."));

        mockMvc.perform(put(BASE)
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Completá al menos una sección de tus términos."));
    }

    @Test
    void guardar_tokenDeOtroUsuario_devuelve403() throws Exception {
        stubUsuarioConRol("intruso", ERol.ARTISTA);
        when(usuarioService.nombreUsuario(7L)).thenReturn("CosmicCrap");

        mockMvc.perform(put(BASE)
                .header("Authorization", "Bearer " + token("intruso", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"informacionGeneral\":\"Hola\"}"))
                .andExpect(status().isForbidden());

        verify(terminosService, never()).guardarGlobal(any(), any());
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

    private TerminosDTO dto() {
        return TerminosDTO.builder()
                .idTerminos(1L)
                .version(2)
                .informacionGeneral("Hola")
                .temas(List.of(TerminoTemaDTO.builder()
                        .idTemaTermino(1L)
                        .nombre("NSFW")
                        .estado("SI")
                        .build()))
                .derechos(List.of())
                .build();
    }
}
