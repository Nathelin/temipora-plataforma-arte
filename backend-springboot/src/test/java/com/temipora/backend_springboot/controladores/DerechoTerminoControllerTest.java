package com.temipora.backend_springboot.controladores;

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
import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = DerechoTerminoController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class DerechoTerminoControllerTest {

    private static final String BASE = "/api/derechos-termino";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private DerechoTerminoRepository derechoTerminoRepository;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(derechoTerminoRepository, never()).findByActivoTrueOrderByOrdenAsc();
    }

    @Test
    void listar_conToken_devuelveElCatalogo() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(derechoTerminoRepository.findByActivoTrueOrderByOrdenAsc())
                .thenReturn(List.of(derecho(1L, "Uso personal", 1)));

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Uso personal"))
                .andExpect(jsonPath("$[0].orden").value(1));
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

    private DerechoTermino derecho(Long id, String nombre, int orden) {
        DerechoTermino derecho = new DerechoTermino();
        derecho.setIdDerechoTermino(id);
        derecho.setNombre(nombre);
        derecho.setOrden(orden);
        return derecho;
    }
}
