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
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.MaterialDTO;
import com.temipora.backend_springboot.servicios.MaterialService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = MaterialAdminController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class MaterialAdminControllerTest {

    private static final String BASE = "/api/admin/materiales";

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
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(materialService, never()).listar();
    }

    @Test
    void listar_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isForbidden());

        verify(materialService, never()).listar();
    }

    @Test
    void listar_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(materialService.listar()).thenReturn(List.of(dto(1L, "Procreate", true)));

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Procreate"))
                .andExpect(jsonPath("$[0].nombreTipoArte").value("Digital"));
    }

    @Test
    void crear_tokenDeAdmin_devuelve201() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(materialService.crear(any())).thenReturn(dto(1L, "Procreate", true));

        mockMvc.perform(post(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Procreate\",\"idTipoArte\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idMaterial").value(1))
                .andExpect(jsonPath("$.nombre").value("Procreate"));
    }

    @Test
    void crear_sinNombre_devuelve400() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);

        mockMvc.perform(post(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"idTipoArte\":1}"))
                .andExpect(status().isBadRequest());

        verify(materialService, never()).crear(any());
    }

    @Test
    void crear_sinTipoDeArte_devuelve400() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);

        mockMvc.perform(post(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Procreate\"}"))
                .andExpect(status().isBadRequest());

        verify(materialService, never()).crear(any());
    }

    @Test
    void modificar_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(materialService.modificar(eq(1L), any())).thenReturn(dto(1L, "Procreate 2", true));

        mockMvc.perform(put(BASE + "/1")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Procreate 2\",\"idTipoArte\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Procreate 2"));
    }

    @Test
    void darDeBaja_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(materialService.darDeBaja(1L)).thenReturn(dto(1L, "Procreate", false));

        mockMvc.perform(delete(BASE + "/1")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void darDeAlta_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(materialService.darDeAlta(1L)).thenReturn(dto(1L, "Procreate", true));

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

    private MaterialDTO dto(Long id, String nombre, boolean activo) {
        return MaterialDTO.builder()
                .idMaterial(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .observaciones(null)
                .activo(activo)
                .idTipoArte(1L)
                .nombreTipoArte("Digital")
                .idMedio(1L)
                .nombreMedio("Digital")
                .build();
    }
}
