package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.temipora.backend_springboot.modelo.CategoriaEtiquetaDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.servicios.CategoriaEtiquetaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = CategoriaEtiquetaController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class CategoriaEtiquetaControllerTest {

    private static final String BASE = "/api/admin/categorias-etiqueta";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private CategoriaEtiquetaService categoriaEtiquetaService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(categoriaEtiquetaService, never()).listar();
    }

    @Test
    void listar_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isForbidden());

        verify(categoriaEtiquetaService, never()).listar();
    }

    @Test
    void listar_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(categoriaEtiquetaService.listar()).thenReturn(List.of(dto(1L, "Estilo", true)));

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Estilo"));
    }

    @Test
    void crear_tokenDeAdmin_devuelve201() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(categoriaEtiquetaService.crear(any())).thenReturn(dto(1L, "Estilo", true));

        mockMvc.perform(post(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Estilo\",\"descripcion\":\"Agrupa estilos\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCategoriaEtiqueta").value(1))
                .andExpect(jsonPath("$.nombre").value("Estilo"));
    }

    @Test
    void crear_sinNombre_devuelve400() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);

        mockMvc.perform(post(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"descripcion\":\"Agrupa estilos\"}"))
                .andExpect(status().isBadRequest());

        verify(categoriaEtiquetaService, never()).crear(any());
    }

    @Test
    void modificar_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(categoriaEtiquetaService.modificar(eq(1L), any())).thenReturn(dto(1L, "Estilo Nuevo", true));

        mockMvc.perform(put(BASE + "/1")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Estilo Nuevo\",\"descripcion\":\"Agrupa estilos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Estilo Nuevo"));
    }

    @Test
    void darDeBaja_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(categoriaEtiquetaService.darDeBaja(1L)).thenReturn(dto(1L, "Estilo", false));

        mockMvc.perform(delete(BASE + "/1")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void darDeAlta_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(categoriaEtiquetaService.darDeAlta(1L)).thenReturn(dto(1L, "Estilo", true));

        mockMvc.perform(post(BASE + "/1/activar")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
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

    private CategoriaEtiquetaDTO dto(Long id, String nombre, boolean activo) {
        return CategoriaEtiquetaDTO.builder()
                .idCategoriaEtiqueta(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .observaciones(null)
                .activo(activo)
                .build();
    }
}
