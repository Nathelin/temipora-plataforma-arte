package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.config.SecurityConfig;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.PaginaReseniasDTO;
import com.temipora.backend_springboot.modelo.ResumenReseniasDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.ReseniaPublicaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = ReseniaPublicaController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class ReseniaPublicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReseniaPublicaService reseniaPublicaService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void listarServicio_sinToken_devuelve200() throws Exception {
        when(reseniaPublicaService.listarServicio(
            eq(1L), any(), any(), eq("RECIENTES"), eq(0), eq(10)))
            .thenReturn(pagina());

        mockMvc.perform(get("/api/servicios/1/resenias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resumen.cantidad").value(1))
            .andExpect(jsonPath("$.pagina.contenido").isArray());
    }

    @Test
    void listarArtista_sinToken_devuelve200() throws Exception {
        when(usuarioService.getByNombreUsuario("ana")).thenReturn(usuario());
        when(reseniaPublicaService.listarArtista(
            eq(5L), any(), any(), eq("RECIENTES"), eq(0), eq(10)))
            .thenReturn(pagina());

        mockMvc.perform(get("/api/perfil/ana/resenias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resumen.cantidad").value(1));
    }

    @Test
    void resumenArtista_sinToken_devuelve200() throws Exception {
        when(usuarioService.getByNombreUsuario("ana")).thenReturn(usuario());
        when(reseniaPublicaService.resumenArtista(5L)).thenReturn(resumen());

        mockMvc.perform(get("/api/perfil/ana/resumen-resenias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidad").value(1));
    }

    private PaginaReseniasDTO pagina() {
        return PaginaReseniasDTO.builder()
            .resumen(resumen())
            .pagina(PaginaDTO.<com.temipora.backend_springboot.modelo.ReseniaPublicaDTO>builder()
                .contenido(List.of())
                .pagina(0)
                .tamanio(10)
                .totalElementos(0)
                .totalPaginas(0)
                .hayMas(false)
                .build())
            .servicios(List.of())
            .build();
    }

    private ResumenReseniasDTO resumen() {
        return ResumenReseniasDTO.builder()
            .cantidad(1)
            .hayDatos(true)
            .build();
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(5L);
        usuario.setNombreUsuario("ana");
        return usuario;
    }
}
