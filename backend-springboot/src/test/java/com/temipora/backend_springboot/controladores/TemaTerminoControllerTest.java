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
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.TemaTermino;
import com.temipora.backend_springboot.repositorios.TemaTerminoRepository;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = TemaTerminoController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class TemaTerminoControllerTest {

    private static final String BASE = "/api/temas-termino";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private TemaTerminoRepository temaTerminoRepository;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(temaTerminoRepository, never()).findByActivoTrueOrderByOrdenAsc();
    }

    @Test
    void listar_conToken_devuelveElCatalogo() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(temaTerminoRepository.findByActivoTrueOrderByOrdenAsc())
                .thenReturn(List.of(tema(1L, "NSFW", 1)));

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("NSFW"))
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

    private TemaTermino tema(Long id, String nombre, int orden) {
        TemaTermino tema = new TemaTermino();
        tema.setIdTemaTermino(id);
        tema.setNombre(nombre);
        tema.setOrden(orden);
        return tema;
    }
}
