package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.config.SecurityConfig;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.RegistroUsuarioDTO;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.FormatoArchivoInvalidoException;
import com.temipora.backend_springboot.servicios.LimpiezaArchivosService;
import com.temipora.backend_springboot.servicios.StorageService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@WebMvcTest(controllers = AuthController.class)
@Import({ SecurityConfig.class, JwtUtils.class })
@TestPropertySource(properties = {
        "jwt.secret=dGVtaXBvcmEtand0LXRlc3Qtc2VjcmV0LTMyYnl0ZXMtb2sh",
        "jwt.time.expiration=3600000",
        "storage.local.dir=target/test-uploads"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private LimpiezaArchivosService limpiezaArchivosService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ---------------------------------------------------------------
    // POST /api/auth/registrarse
    // ---------------------------------------------------------------

    @Test
    void registrarse_contrasenasNoCoinciden_devuelve400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post("/api/auth/registrarse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registroJson("Secreta123", "OtraClave1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Las contraseñas no coinciden."));

        verify(usuarioService, never()).existeNombreUsuario(any());
        verify(usuarioService, never()).registrarUsuario(any());
    }

    @Test
    void registrarse_nombreDeUsuarioEnUso_devuelve400() throws Exception {
        when(usuarioService.existeNombreUsuario("cosmiq")).thenReturn(true);

        mockMvc.perform(post("/api/auth/registrarse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registroJson("Secreta123", "Secreta123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre de usuario ya está en uso."));

        verify(usuarioService, never()).registrarUsuario(any());
    }

    @Test
    void registrarse_datosValidos_devuelve201ConUsuario() throws Exception {
        when(usuarioService.existeNombreUsuario("cosmiq")).thenReturn(false);
        when(usuarioService.registrarUsuario(any(RegistroUsuarioDTO.class)))
                .thenReturn(Usuario.builder()
                        .idUsuario(1L)
                        .nombreUsuario("cosmiq")
                        .correo("cosmiq@mail.com")
                        .rol(Rol.builder().idRol(3L).nombre(ERol.CLIENTE).build())
                        .build());

        mockMvc.perform(post("/api/auth/registrarse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registroJson("Secreta123", "Secreta123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.nombreUsuario").value("cosmiq"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        ArgumentCaptor<RegistroUsuarioDTO> captor = ArgumentCaptor
                .forClass(RegistroUsuarioDTO.class);
        verify(usuarioService).registrarUsuario(captor.capture());
        assertThat(captor.getValue().getNombreUsuario()).isEqualTo("cosmiq");
        assertThat(captor.getValue().isAcceptaTOS()).isTrue();
    }

    @Test
    void registrarse_bodyVacio_devuelve400PorValidacion() throws Exception {
        mockMvc.perform(post("/api/auth/registrarse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(usuarioService, never()).registrarUsuario(any());
    }

    // ---------------------------------------------------------------
    // POST /api/auth/upload
    // ---------------------------------------------------------------

    @Test
    void upload_archivoValido_devuelve200ConUrl() throws Exception {
        when(storageService.guardar(any())).thenReturn("/uploads/abc.webp");

        mockMvc.perform(multipart("/api/upload")
                .file(new MockMultipartFile("archivo", "foto.png", "image/png",
                        "imagen".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/uploads/abc.webp"));
    }

    @Test
    void upload_formatoInvalido_devuelve400ConMensaje() throws Exception {
        when(storageService.guardar(any())).thenThrow(new FormatoArchivoInvalidoException());

        mockMvc.perform(multipart("/api/upload")
                .file(new MockMultipartFile("archivo", "nota.txt", "text/plain",
                        "texto".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El formato es inválido."));
    }

    @Test
    void upload_servicioDevuelveNull_devuelveUrlVacia() throws Exception {
        when(storageService.guardar(any())).thenReturn(null);

        mockMvc.perform(multipart("/api/upload")
                .file(new MockMultipartFile("archivo", "foto.png", "image/png",
                        "imagen".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(""));
    }

    // ---------------------------------------------------------------
    // DELETE /api/upload
    // ---------------------------------------------------------------

    @Test
    void eliminarArchivos_listaValida_devuelveConteoDeEliminados() throws Exception {
        when(limpiezaArchivosService.eliminar(anyList())).thenReturn(2);

        mockMvc.perform(delete("/api/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"/uploads/a.webp\",\"/uploads/b.webp\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eliminados").value(2));

        verify(limpiezaArchivosService).eliminar(anyList());
    }

    @Test
    void eliminarArchivos_listaVacia_devuelve400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(delete("/api/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debes indicar al menos una imagen."));

        verify(limpiezaArchivosService, never()).eliminar(anyList());
    }

    @Test
    void eliminarArchivos_ningunoReferenciado_devuelveCero() throws Exception {
        when(limpiezaArchivosService.eliminar(anyList())).thenReturn(0);

        mockMvc.perform(delete("/api/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"/uploads/abc.webp\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eliminados").value(0));
    }

    // ---------------------------------------------------------------
    // GET /api/checkNombreUsuario
    // ---------------------------------------------------------------

    @Test
    void checkNombreUsuario_disponible_devuelveTrue() throws Exception {
        when(usuarioService.existeNombreUsuario("cosmiq")).thenReturn(false);

        mockMvc.perform(get("/api/checkNombreUsuario").param("nombreUsuario", "cosmiq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    void checkNombreUsuario_enUso_devuelveFalse() throws Exception {
        when(usuarioService.existeNombreUsuario("cosmiq")).thenReturn(true);

        mockMvc.perform(get("/api/checkNombreUsuario").param("nombreUsuario", "cosmiq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false));
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private String registroJson(String contrasenia, String confirmarContrasenia) {
        return "{\"nombreUsuario\":\"cosmiq\",\"correo\":\"cosmiq@mail.com\","
                + "\"contrasenia\":\"" + contrasenia + "\","
                + "\"confirmarContrasenia\":\"" + confirmarContrasenia + "\","
                + "\"acceptaTOS\":true,\"rol\":\"CLIENTE\"}";
    }
}
