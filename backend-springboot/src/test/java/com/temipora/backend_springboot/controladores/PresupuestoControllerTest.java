package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.temipora.backend_springboot.config.ManejoGlobalErrores;
import com.temipora.backend_springboot.config.SecurityConfig;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.SolicitudDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.ConflictoException;
import com.temipora.backend_springboot.servicios.PresupuestoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = PresupuestoController.class)
@Import({ SecurityConfig.class, JwtUtils.class, ManejoGlobalErrores.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class PresupuestoControllerTest {

    private static final String JSON_VALIDO = """
        {
          "tiempoEntregaValor": 2,
          "tiempoEntregaUnidad": "SEMANAS",
          "items": [
            {
              "idItemSolicitud": 10,
              "nota": "Incluye dos personajes",
              "recargos": [
                {"descripcion": "Personaje extra", "tipo": "PORCENTAJE", "valor": 50}
              ]
            }
          ]
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private PresupuestoService presupuestoService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void crear_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/api/solicitudes-servicio/1/presupuesto")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isUnauthorized());

        verify(presupuestoService, never()).crear(any(), any(), any());
    }

    @Test
    void crear_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);

        mockMvc.perform(post("/api/solicitudes-servicio/1/presupuesto")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isForbidden());

        verify(presupuestoService, never()).crear(any(), any(), any());
    }

    @Test
    void crear_tokenDeArtista_devuelve201() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(usuarioService.getByNombreUsuario("artista1")).thenReturn(usuario(7L));
        when(presupuestoService.crear(eq(1L), eq(7L), any())).thenReturn(dto());

        mockMvc.perform(post("/api/solicitudes-servicio/1/presupuesto")
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idSolicitud").value(1));
    }

    @Test
    void crear_sinDibujos_devuelve400() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);

        mockMvc.perform(post("/api/solicitudes-servicio/1/presupuesto")
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tiempoEntregaValor": 2, "tiempoEntregaUnidad": "DIAS", "items": []}
                    """))
                .andExpect(status().isBadRequest());

        verify(presupuestoService, never()).crear(any(), any(), any());
    }

    @Test
    void crear_sinUnidadDeEntrega_devuelve400() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);

        mockMvc.perform(post("/api/solicitudes-servicio/1/presupuesto")
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tiempoEntregaValor": 2, "items": [{"idItemSolicitud": 10}]}
                    """))
                .andExpect(status().isBadRequest());

        verify(presupuestoService, never()).crear(any(), any(), any());
    }

    @Test
    void crear_sinCupos_devuelve409() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(usuarioService.getByNombreUsuario("artista1")).thenReturn(usuario(7L));
        when(presupuestoService.crear(eq(1L), eq(7L), any()))
            .thenThrow(new ConflictoException("No hay cupos libres."));

        mockMvc.perform(post("/api/solicitudes-servicio/1/presupuesto")
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No hay cupos libres."));
    }

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
