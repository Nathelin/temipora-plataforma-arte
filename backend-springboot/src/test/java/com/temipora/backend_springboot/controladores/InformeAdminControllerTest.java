package com.temipora.backend_springboot.controladores;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.config.SecurityConfig;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.InformeAdminDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTotalesDTO;
import com.temipora.backend_springboot.modelo.InformePeriodoDTO;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.InformeAdminPdfService;
import com.temipora.backend_springboot.servicios.InformeAdminService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = InformeAdminController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class InformeAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private InformeAdminService informeAdminService;

    @MockitoBean
    private InformeAdminPdfService informeAdminPdfService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void ventas_tokenDeAdmin_devuelveElInforme() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(informeAdminService.generar(any(), any(), any(), any()))
            .thenReturn(informe());

        mockMvc.perform(get("/api/admin/informes/ventas")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moneda").value("ARS"));
    }

    @Test
    void ventasPdf_tokenDeAdmin_devuelveElPdf() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(informeAdminService.generar(any(), any(), any(), any()))
            .thenReturn(informe());
        when(informeAdminPdfService.generar(any()))
            .thenReturn("%PDF".getBytes());

        mockMvc.perform(get("/api/admin/informes/ventas/pdf")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    containsString("attachment")));
    }

    @Test
    void ventas_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente", ERol.CLIENTE);

        mockMvc.perform(get("/api/admin/informes/ventas")
                .header("Authorization", "Bearer " + token("cliente", ERol.CLIENTE)))
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

    private InformeAdminDTO informe() {
        return new InformeAdminDTO(
            new InformePeriodoDTO(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), 30, "DIA"),
            "ARS",
            "TODOS",
            LocalDateTime.now(),
            new InformeAdminTotalesDTO(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO),
            null,
            0,
            List.of(),
            List.of(),
            List.of(),
            List.of());
    }
}
