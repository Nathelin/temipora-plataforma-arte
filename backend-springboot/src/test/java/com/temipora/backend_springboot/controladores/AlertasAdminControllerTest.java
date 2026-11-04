package com.temipora.backend_springboot.controladores;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

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
import com.temipora.backend_springboot.modelo.AlertaAdminDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.AlertasAdminService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = AlertasAdminController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class AlertasAdminControllerTest {

    private static final String BASE = "/api/admin/resumen/alertas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private AlertasAdminService alertasService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void alertas_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(alertasService, never()).listar();
    }

    @Test
    void alertas_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isForbidden());

        verify(alertasService, never()).listar();
    }

    @Test
    void alertas_tokenDeAdmin_devuelveLaLista() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(alertasService.listar()).thenReturn(List.of(new AlertaAdminDTO(
            "LOGIN_FALLIDO_USUARIO",
            "ALTA",
            "Posible fuerza bruta",
            "El usuario @ana acumuló 7 intentos.",
            7,
            LocalDateTime.now().minusMinutes(15),
            LocalDateTime.now(),
            "LOGIN_FALLIDO",
            "ana")));

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("LOGIN_FALLIDO_USUARIO"))
                .andExpect(jsonPath("$[0].severidad").value("ALTA"))
                .andExpect(jsonPath("$[0].actorNombre").value("ana"))
                .andExpect(jsonPath("$[0].cantidad").value(7));
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
