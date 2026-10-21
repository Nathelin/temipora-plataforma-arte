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

import java.math.BigDecimal;
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
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.ServicioService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = ServicioController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class ServicioControllerTest {

    private static final String JSON_VALIDO = "{\"idTipoServicio\":1,\"idMedio\":2,\"nombre\":\"Ilustración\"," +
            "\"descripcion\":\"desc\",\"precioBase\":100,\"tiempoEntregaValor\":2,\"tiempoEntregaUnidad\":\"SEMANAS\"," +
            "\"cupos\":5,\"imagenPortada\":\"portada.png\",\"imagenes\":[\"a.png\"]}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private ServicioService servicioService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void misServicios_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/mis-servicios")).andExpect(status().isUnauthorized());

        verify(servicioService, never()).listarMios(any());
    }

    @Test
    void misServicios_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);

        mockMvc.perform(get("/api/mis-servicios")
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void misServicios_tokenDeArtista_devuelve200() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(usuarioService.getByNombreUsuario("artista1")).thenReturn(usuario(5L));
        when(servicioService.listarMios(5L)).thenReturn(List.of(dto()));

        mockMvc.perform(get("/api/mis-servicios")
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Ilustración"));
    }

    @Test
    void crear_tokenDeArtista_devuelve201() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(usuarioService.getByNombreUsuario("artista1")).thenReturn(usuario(5L));
        when(servicioService.crear(eq(5L), any())).thenReturn(dto());

        mockMvc.perform(post("/api/servicios")
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idServicio").value(1))
                .andExpect(jsonPath("$.nombre").value("Ilustración"));
    }

    @Test
    void crear_sinCamposObligatorios_devuelve400() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);

        mockMvc.perform(post("/api/servicios")
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verify(servicioService, never()).crear(any(), any());
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
        return Usuario.builder().idUsuario(id).nombreUsuario("artista1").build();
    }

    private ServicioDTO dto() {
        return ServicioDTO.builder()
                .idServicio(1L)
                .nombre("Ilustración")
                .precioBase(new BigDecimal("100.00"))
                .tiempoEntregaValor(2)
                .tiempoEntregaUnidad("SEMANAS")
                .tiempoEntregaDias(14)
                .cupos(5)
                .estado(true)
                .imagenes(List.of("a.png"))
                .build();
    }
}
