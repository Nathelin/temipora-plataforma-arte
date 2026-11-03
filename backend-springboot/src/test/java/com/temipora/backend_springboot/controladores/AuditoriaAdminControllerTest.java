package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.temipora.backend_springboot.modelo.CambioAuditoriaDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EventoAuditoriaDTO;
import com.temipora.backend_springboot.modelo.OpcionesAuditoriaDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.AuditoriaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = AuditoriaAdminController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class AuditoriaAdminControllerTest {

    private static final String BASE = "/api/admin/auditoria";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private AuditoriaService auditoriaService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verify(auditoriaService, never()).listar(any(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void listar_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listar_tokenDeAdmin_devuelveEventos() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(auditoriaService.listar(null, null, null, null, null, 0, 20))
                .thenReturn(pagina());

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].resumen").value("Aprobó la etiqueta"))
                .andExpect(jsonPath("$.contenido[0].cambios[0].campo").value("estado"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void listar_conFiltrosYFechas_losConvierteYDelega() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59, 59);
        when(auditoriaService.listar("admin", "Etiqueta", "ETIQUETA_APROBAR", desde, hasta, 2, 50))
                .thenReturn(pagina());

        mockMvc.perform(get(BASE)
                .param("actor", "admin")
                .param("entidad", "Etiqueta")
                .param("accion", "ETIQUETA_APROBAR")
                .param("desde", "2026-01-01")
                .param("hasta", "2026-01-31")
                .param("pagina", "2")
                .param("tamanio", "50")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk());

        verify(auditoriaService).listar("admin", "Etiqueta", "ETIQUETA_APROBAR", desde, hasta, 2, 50);
    }

    @Test
    void opciones_tokenDeAdmin_devuelveListas() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(auditoriaService.opciones())
                .thenReturn(new OpcionesAuditoriaDTO(List.of("Etiqueta"), List.of("ETIQUETA_APROBAR")));

        mockMvc.perform(get(BASE + "/opciones")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entidades[0]").value("Etiqueta"))
                .andExpect(jsonPath("$.acciones[0]").value("ETIQUETA_APROBAR"));
    }

    @Test
    void opciones_tokenDeCliente_devuelve403() throws Exception {
        stubUsuarioConRol("cliente42", ERol.CLIENTE);

        mockMvc.perform(get(BASE + "/opciones")
                .header("Authorization", "Bearer " + token("cliente42", ERol.CLIENTE)))
                .andExpect(status().isForbidden());

        verify(auditoriaService, never()).opciones();
    }

    private PaginaDTO<EventoAuditoriaDTO> pagina() {
        EventoAuditoriaDTO evento = new EventoAuditoriaDTO(
            1L,
            LocalDateTime.now(),
            1L,
            "ADMIN",
            "admin",
            "ETIQUETA_APROBAR",
            "Etiqueta",
            "3",
            "Aprobó la etiqueta",
            List.of(new CambioAuditoriaDTO("estado", "Estado", "PENDIENTE", "VALIDA")),
            null,
            "127.0.0.1",
            "req-1",
            "ADMIN",
            "OK");
        return PaginaDTO.<EventoAuditoriaDTO>builder()
            .contenido(List.of(evento))
            .pagina(0)
            .tamanio(20)
            .totalElementos(1)
            .totalPaginas(1)
            .hayMas(false)
            .build();
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
