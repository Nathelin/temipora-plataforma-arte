package com.temipora.backend_springboot.controladores;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.ResumenNegocioDTO;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.ResumenAdminService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = ResumenAdminController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class ResumenAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private ResumenAdminService resumenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void negocio_tokenDeAdmin_devuelveElResumen() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(resumenService.negocio()).thenReturn(new ResumenNegocioDTO(
            "2026-11",
            "ARS",
            new BigDecimal("1500.00"),
            new BigDecimal("1000.00"),
            new BigDecimal("150.00"),
            new BigDecimal("100.00"),
            2,
            1,
            new BigDecimal("750.00"),
            new BigDecimal("1000.00")));

        mockMvc.perform(get("/api/admin/resumen/negocio")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("2026-11"))
                .andExpect(jsonPath("$.moneda").value("ARS"))
                .andExpect(jsonPath("$.volumen").value(1500.00))
                .andExpect(jsonPath("$.comision").value(150.00))
                .andExpect(jsonPath("$.operaciones").value(2));
    }

    @Test
    void negocio_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(get("/api/admin/resumen/negocio")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isForbidden());
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
}
