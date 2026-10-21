package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.temipora.backend_springboot.modelo.TerminosDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.FormularioService;
import com.temipora.backend_springboot.servicios.ServicioService;
import com.temipora.backend_springboot.servicios.TerminosService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = ServicioConfiguracionController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class ServicioConfiguracionControllerTest {

    private static final String BASE = "/api/servicios/7";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private ServicioService servicioService;

    @MockitoBean
    private FormularioService formularioService;

    @MockitoBean
    private TerminosService terminosService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void obtenerFormulario_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE + "/formulario-pedidos"))
            .andExpect(status().isUnauthorized());

        verify(formularioService, never()).obtenerServicio(any(), any());
    }

    @Test
    void obtenerFormulario_tokenValido_devuelve200() throws Exception {
        autenticarArtista();
        when(formularioService.obtenerServicio(7L, 5L)).thenReturn(formulario());

        mockMvc.perform(get(BASE + "/formulario-pedidos")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.propio").value(true))
            .andExpect(jsonPath("$.preguntas[0].enunciado").value("¿Qué querés?"));
    }

    @Test
    void guardarFormulario_tokenValido_devuelve200() throws Exception {
        autenticarArtista();
        when(formularioService.guardarServicio(eq(7L), eq(5L), any())).thenReturn(formulario());

        mockMvc.perform(put(BASE + "/formulario-pedidos")
                .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntas\":[{\"idTipoCampo\":1,\"enunciado\":\"¿Qué querés?\",\"obligatoria\":true}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.propio").value(true));
    }

    @Test
    void eliminarFormulario_tokenValido_devuelve204() throws Exception {
        autenticarArtista();

        mockMvc.perform(delete(BASE + "/formulario-pedidos")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isNoContent());

        verify(formularioService).eliminarServicio(7L, 5L);
    }

    @Test
    void obtenerTerminos_tokenValido_devuelve200() throws Exception {
        autenticarArtista();
        when(terminosService.obtenerServicio(7L, 5L)).thenReturn(terminos());

        mockMvc.perform(get(BASE + "/terminos")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.propio").value(true))
            .andExpect(jsonPath("$.informacionGeneral").value("Hola"));
    }

    @Test
    void guardarTerminos_tokenValido_devuelve200() throws Exception {
        autenticarArtista();
        when(terminosService.guardarServicio(eq(7L), eq(5L), any())).thenReturn(terminos());

        mockMvc.perform(put(BASE + "/terminos")
                .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"informacionGeneral\":\"Hola\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.propio").value(true));
    }

    @Test
    void eliminarTerminos_tokenValido_devuelve204() throws Exception {
        autenticarArtista();

        mockMvc.perform(delete(BASE + "/terminos")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isNoContent());

        verify(terminosService).eliminarServicio(7L, 5L);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void autenticarArtista() {
        when(userDetailsService.loadUserByUsername("artista1"))
            .thenReturn(User.withUsername("artista1")
                .password("{noop}prueba")
                .authorities("ROLE_" + ERol.ARTISTA.name())
                .build());
        when(usuarioService.getByNombreUsuario("artista1")).thenReturn(usuario(5L));
    }

    private String token() {
        return jwtUtils.generarTokenAcceso("artista1", ERol.ARTISTA.name());
    }

    private Usuario usuario(Long id) {
        return Usuario.builder().idUsuario(id).nombreUsuario("artista1").build();
    }

    private FormularioDTO formulario() {
        return FormularioDTO.builder()
            .idFormulario(3L)
            .idServicio(7L)
            .propio(true)
            .preguntas(List.of(PreguntaDTO.builder()
                .enunciado("¿Qué querés?")
                .codigoTipo("TEXTO")
                .build()))
            .build();
    }

    private TerminosDTO terminos() {
        return TerminosDTO.builder()
            .idTerminos(4L)
            .idServicio(7L)
            .propio(true)
            .version(1)
            .informacionGeneral("Hola")
            .temas(List.of())
            .derechos(List.of())
            .build();
    }
}
