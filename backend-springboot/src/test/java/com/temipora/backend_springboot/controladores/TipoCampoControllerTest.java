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
import com.temipora.backend_springboot.modelo.TipoCampo;
import com.temipora.backend_springboot.repositorios.TipoCampoRepository;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = TipoCampoController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class TipoCampoControllerTest {

    private static final String BASE = "/api/tipos-campo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private TipoCampoRepository tipoCampoRepository;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(tipoCampoRepository, never()).findByActivoTrueOrderByNombreAsc();
    }

    @Test
    void listar_conToken_devuelveElCatalogo() throws Exception {
        stubUsuarioConRol("artista1", ERol.ARTISTA);
        when(tipoCampoRepository.findByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(tipo(1L, "TEXTO", "Bloque de texto")));

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("artista1", ERol.ARTISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("TEXTO"))
                .andExpect(jsonPath("$[0].nombre").value("Bloque de texto"));
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

    private TipoCampo tipo(Long id, String codigo, String nombre) {
        TipoCampo tipo = new TipoCampo();
        tipo.setIdTipoCampo(id);
        tipo.setCodigo(codigo);
        tipo.setNombre(nombre);
        return tipo;
    }
}
