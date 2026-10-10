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
import com.temipora.backend_springboot.modelo.TipoProductoDTO;
import com.temipora.backend_springboot.servicios.TipoProductoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = TipoProductoController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class TipoProductoControllerTest {

    private static final String BASE = "/api/admin/tipos-producto";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private TipoProductoService tipoProductoService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(tipoProductoService, never()).listar();
    }

    @Test
    void listar_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isForbidden());

        verify(tipoProductoService, never()).listar();
    }

    @Test
    void listar_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(tipoProductoService.listar()).thenReturn(List.of(dto(1L, "Lienzo")));

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Lienzo"));
    }

    @Test
    void crear_tokenDeAdmin_devuelve201() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(tipoProductoService.crear(any())).thenReturn(dto(1L, "Lienzo"));

        mockMvc.perform(post(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idTipoProducto").value(1))
                .andExpect(jsonPath("$.nombre").value("Lienzo"));
    }

    @Test
    void modificar_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(tipoProductoService.modificar(eq(1L), any())).thenReturn(dto(1L, "Lienzo Actualizado"));

        mockMvc.perform(put(BASE + "/1")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestValido()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Lienzo Actualizado"));
    }

    @Test
    void darDeBaja_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(tipoProductoService.darDeBaja(1L))
                .thenReturn(dtoInactivo(1L, "Lienzo"));

        mockMvc.perform(delete(BASE + "/1")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void darDeAlta_tokenDeAdmin_devuelve200() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(tipoProductoService.darDeAlta(1L)).thenReturn(dto(1L, "Lienzo"));

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

    private TipoProductoDTO dto(Long id, String nombre) {
        return TipoProductoDTO.builder()
                .idTipoProducto(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .activo(true)
                .build();
    }

    private TipoProductoDTO dtoInactivo(Long id, String nombre) {
        return TipoProductoDTO.builder()
                .idTipoProducto(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .activo(false)
                .build();
    }

    private String requestValido() {
        return "{\"nombre\":\"Lienzo\",\"descripcion\":\"Una descripcion\","
                + "\"observaciones\":\"ninguna\"}";
    }
}
