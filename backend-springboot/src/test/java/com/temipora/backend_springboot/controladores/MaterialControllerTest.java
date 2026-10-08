package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.MaterialDTO;
import com.temipora.backend_springboot.servicios.MaterialService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = MaterialController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class MaterialControllerTest {

    private static final String BASE = "/api/materiales";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private MaterialService materialService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void selector_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(materialService, never()).selector(any());
    }

    @Test
    void selector_conToken_devuelve200() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        when(materialService.selector(1L)).thenReturn(List.of(dto(1L, "Procreate")));

        mockMvc.perform(get(BASE)
                .param("idMedio", "1")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Procreate"));
    }

    @Test
    void selector_sinMedio_devuelve200ConTodo() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);
        when(materialService.selector(null)).thenReturn(List.of());

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void selector_medioInvalido_devuelve400() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(get(BASE)
                .param("idMedio", "abc")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Medio no válido: abc"));

        verify(materialService, never()).selector(any());
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

    private MaterialDTO dto(Long id, String nombre) {
        return MaterialDTO.builder()
                .idMaterial(id)
                .nombre(nombre)
                .activo(true)
                .idTipoArte(1L)
                .nombreTipoArte("Digital")
                .idMedio(1L)
                .nombreMedio("Digital")
                .build();
    }
}
