package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
import com.temipora.backend_springboot.modelo.CotizacionConfigDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.servicios.CotizacionService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = CotizacionAdminController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class CotizacionAdminControllerTest {

    private static final String BASE = "/api/admin/cotizacion";
    private static final String JSON_VALIDO = "{\"apiHabilitada\":true,\"idMonedaOrigen\":2," +
            "\"idMonedaDestino\":1,\"factorManual\":1500.00}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private CotizacionService cotizacionService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void obtener_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(cotizacionService, never()).obtenerEstado();
    }

    @Test
    void obtener_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente1", ERol.CLIENTE);

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("cliente1", ERol.CLIENTE)))
                .andExpect(status().isForbidden());

        verify(cotizacionService, never()).obtenerEstado();
    }

    @Test
    void obtener_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(cotizacionService.obtenerEstado()).thenReturn(dto());

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monedaOrigen").value("USD"))
                .andExpect(jsonPath("$.monedaDestino").value("ARS"))
                .andExpect(jsonPath("$.apiHabilitada").value(true));
    }

    @Test
    void actualizar_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(cotizacionService.actualizar(any())).thenReturn(dto());

        mockMvc.perform(put(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factorVigente").value(1500.00));
    }

    @Test
    void actualizar_sinCamposObligatorios_devuelve400() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);

        mockMvc.perform(put(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verify(cotizacionService, never()).actualizar(any());
    }

    @Test
    void refrescar_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(cotizacionService.refrescarDesdeApi()).thenReturn(dto());

        mockMvc.perform(post(BASE + "/refrescar")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origenFactor").value("API"));
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

    private CotizacionConfigDTO dto() {
        return CotizacionConfigDTO.builder()
                .apiHabilitada(true)
                .idMonedaOrigen(2L)
                .monedaOrigen("USD")
                .idMonedaDestino(1L)
                .monedaDestino("ARS")
                .factorManual(new BigDecimal("1500.00"))
                .factorVigente(new BigDecimal("1500.00"))
                .origenFactor("API")
                .build();
    }
}
