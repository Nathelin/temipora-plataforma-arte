package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import com.temipora.backend_springboot.modelo.CategoriaAuditoriaDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EventoAuditoriaDTO;
import com.temipora.backend_springboot.modelo.FiltroAuditoria;
import com.temipora.backend_springboot.modelo.OpcionesAuditoriaDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.ResumenAuditoriaDTO;
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

        verify(auditoriaService, never()).listar(any(FiltroAuditoria.class), anyInt(), anyInt());
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
        when(auditoriaService.listar(any(FiltroAuditoria.class), eq(0), eq(20)))
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
        when(auditoriaService.listar(
                any(FiltroAuditoria.class), eq(2), eq(50)))
                .thenReturn(pagina());

        mockMvc.perform(get(BASE)
                .param("actor", "admin")
                .param("origen", "ADMIN")
                .param("entidad", "Etiqueta")
                .param("entidadId", "3")
                .param("accion", "ETIQUETA_APROBAR")
                .param("requestId", "req-1")
                .param("texto", "etiqueta")
                .param("categoria", "CATALOGO")
                .param("critica", "true")
                .param("desde", "2026-01-01")
                .param("hasta", "2026-01-31")
                .param("pagina", "2")
                .param("tamanio", "50")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk());

        ArgumentCaptor<FiltroAuditoria> captor =
            ArgumentCaptor.forClass(FiltroAuditoria.class);
        verify(auditoriaService).listar(captor.capture(), eq(2), eq(50));
        FiltroAuditoria filtro = captor.getValue();
        assertThat(filtro.actor()).isEqualTo("admin");
        assertThat(filtro.origen()).isEqualTo("ADMIN");
        assertThat(filtro.entidad()).isEqualTo("Etiqueta");
        assertThat(filtro.entidadId()).isEqualTo("3");
        assertThat(filtro.accion()).isEqualTo("ETIQUETA_APROBAR");
        assertThat(filtro.requestId()).isEqualTo("req-1");
        assertThat(filtro.texto()).isEqualTo("etiqueta");
        assertThat(filtro.categoria()).isEqualTo("CATALOGO");
        assertThat(filtro.critica()).isTrue();
        assertThat(filtro.desde()).isEqualTo(desde);
        assertThat(filtro.hasta()).isEqualTo(hasta);
    }

    @Test
    void listar_conFechaHoraConservaLaPrecision() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(auditoriaService.listar(
                any(FiltroAuditoria.class), eq(0), eq(20)))
                .thenReturn(pagina());

        mockMvc.perform(get(BASE)
                .param("desde", "2026-01-01T10:30:00")
                .param("hasta", "2026-01-01T11:45:00")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk());

        ArgumentCaptor<FiltroAuditoria> captor =
            ArgumentCaptor.forClass(FiltroAuditoria.class);
        verify(auditoriaService).listar(captor.capture(), eq(0), eq(20));
        assertThat(captor.getValue().desde())
            .isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 30));
        assertThat(captor.getValue().hasta())
            .isEqualTo(LocalDateTime.of(2026, 1, 1, 11, 45));
    }

    @Test
    void opciones_tokenDeAdmin_devuelveListas() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(auditoriaService.opciones())
                .thenReturn(new OpcionesAuditoriaDTO(
                    List.of("Etiqueta"),
                    List.of("ETIQUETA_APROBAR"),
                    List.of(new CategoriaAuditoriaDTO("CATALOGO", "Catálogo"))));

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
        return PaginaDTO.<EventoAuditoriaDTO>builder()
            .contenido(List.of(evento()))
            .pagina(0)
            .tamanio(20)
            .totalElementos(1)
            .totalPaginas(1)
            .hayMas(false)
            .build();
    }

    private EventoAuditoriaDTO evento() {
        return new EventoAuditoriaDTO(
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
            "req-1",
            "ADMIN",
            "OK",
            "CATALOGO",
            false);
    }

    @Test
    void resumen_tokenDeAdmin_devuelveConteos() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(auditoriaService.resumen(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new ResumenAuditoriaDTO(10, 4, 2));

        mockMvc.perform(get(BASE + "/resumen")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.criticas").value(4))
                .andExpect(jsonPath("$.loginFallido").value(2));
    }

    @Test
    void obtener_tokenDeAdmin_devuelveElEvento() throws Exception {
        stubUsuarioConRol("admin", ERol.ADMIN);
        when(auditoriaService.obtener(1L)).thenReturn(evento());

        mockMvc.perform(get(BASE + "/1")
                .header("Authorization", "Bearer " + token("admin", ERol.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen").value("Aprobó la etiqueta"))
                .andExpect(jsonPath("$.categoria").value("CATALOGO"))
                .andExpect(jsonPath("$.critica").value(false));
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
