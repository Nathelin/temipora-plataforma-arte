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
import com.temipora.backend_springboot.modelo.FormularioDTO;
import com.temipora.backend_springboot.modelo.PreguntaDTO;
import com.temipora.backend_springboot.servicios.FormularioService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = FormularioController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class FormularioControllerTest {

    private static final String BASE = "/api/usuarios/7/formulario-pedidos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private FormularioService formularioService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void obtener_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(formularioService, never()).obtenerGlobal(any());
    }

    @Test
    void obtener_tokenDeOtroUsuario_devuelve403() throws Exception {
        stubUsuarioConRol("intruso", ERol.ARTISTA);
        when(usuarioService.nombreUsuario(7L)).thenReturn("CosmicCrap");

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("intruso", ERol.ARTISTA)))
                .andExpect(status().isForbidden());

        verify(formularioService, never()).obtenerGlobal(any());
    }

    @Test
    void obtener_tokenValido_devuelve200() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(usuarioService.nombreUsuario(7L)).thenReturn("artista1");
        when(formularioService.obtenerGlobal(7L)).thenReturn(dto());

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esSistema").value(false))
                .andExpect(jsonPath("$.preguntas[0].enunciado").value("¿Qué querés?"));
    }

    @Test
    void guardar_tokenValido_devuelve200() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(usuarioService.nombreUsuario(7L)).thenReturn("artista1");
        when(formularioService.guardarGlobal(eq(7L), any())).thenReturn(dto());

        mockMvc.perform(put(BASE)
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntas\":[{\"idTipoCampo\":1,\"enunciado\":\"¿Qué querés?\","+
                        "\"obligatoria\":true,\"opciones\":[]}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preguntas[0].enunciado").value("¿Qué querés?"));
    }

    @Test
    void guardar_sinPreguntas_devuelve400() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);

        mockMvc.perform(put(BASE)
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntas\":[]}"))
                .andExpect(status().isBadRequest());

        verify(formularioService, never()).guardarGlobal(any(), any());
    }

    @Test
    void guardar_tokenDeOtroUsuario_devuelve403() throws Exception {
        stubUsuarioConRol("intruso", ERol.ARTISTA);
        when(usuarioService.nombreUsuario(7L)).thenReturn("CosmicCrap");

        mockMvc.perform(put(BASE)
                .header("Authorization", "Bearer " + token("intruso", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntas\":[{\"idTipoCampo\":1,\"enunciado\":\"X\",\"opciones\":[]}]}"))
                .andExpect(status().isForbidden());

        verify(formularioService, never()).guardarGlobal(any(), any());
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

    private FormularioDTO dto() {
        return FormularioDTO.builder()
                .idFormulario(1L)
                .esSistema(false)
                .preguntas(List.of(PreguntaDTO.builder()
                        .idPregunta(1L)
                        .idTipoCampo(1L)
                        .codigoTipo("TEXTO")
                        .enunciado("¿Qué querés?")
                        .obligatoria(true)
                        .opciones(List.of())
                        .build()))
                .build();
    }
}
