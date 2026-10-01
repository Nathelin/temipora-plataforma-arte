package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.temipora.backend_springboot.modelo.ActualizarPerfilDTO;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.RegistroUsuarioDTO;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.repositorios.RolRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    // ---------------------------------------------------------------
    // registrarUsuario
    // ---------------------------------------------------------------

    @Test
    void registrarUsuario_conRolAdmin_lanzaExcepcionYNoGuarda() {
        RegistroUsuarioDTO dto = registroConRol(ERol.ADMIN);

        assertThatThrownBy(() -> usuarioService.registrarUsuario(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No se puede auto-registrar un usuario con rol ADMIN");

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(rolRepository, never()).findByNombre(any(ERol.class));
    }

    @Test
    void registrarUsuario_rolInexistenteEnBase_lanzaIllegalState() {
        RegistroUsuarioDTO dto = registroConRol(ERol.ARTISTA);
        when(rolRepository.findByNombre(ERol.ARTISTA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.registrarUsuario(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("El rol ARTISTA no existe");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void registrarUsuario_datosValidos_guardaConPasswordHasheadoYEstadoActivo() {
        RegistroUsuarioDTO dto = registroConRol(ERol.CLIENTE);
        dto.setNombre("Cosmo");
        dto.setApellido("Draws");
        dto.setBiografia("Artista emergente");

        Rol rolCliente = Rol.builder().idRol(3L).nombre(ERol.CLIENTE).build();
        when(rolRepository.findByNombre(ERol.CLIENTE)).thenReturn(Optional.of(rolCliente));
        when(passwordEncoder.encode("secreta123")).thenReturn("$2a$10$hasheado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario guardado = usuarioService.registrarUsuario(dto);

        assertThat(guardado.getNombreUsuario()).isEqualTo("cosmiq");
        assertThat(guardado.getCorreo()).isEqualTo("cosmiq@mail.com");
        assertThat(guardado.getContrasenia()).isEqualTo("$2a$10$hasheado");
        assertThat(guardado.isEstadoCuenta()).isTrue();
        assertThat(guardado.getFechaCreacion()).isNotNull();
        assertThat(guardado.getRol()).isEqualTo(rolCliente);
        assertThat(guardado.getNombre()).isEqualTo("Cosmo");
        assertThat(guardado.getBiografia()).isEqualTo("Artista emergente");
        verify(passwordEncoder).encode("secreta123");
    }

    // ---------------------------------------------------------------
    // existencia / lectura
    // ---------------------------------------------------------------

    @Test
    void existeNombreUsuario_delegaEnElRepositorio() {
        when(usuarioRepository.existsByNombreUsuarioIgnoreCase("Cosmiq")).thenReturn(true);

        assertThat(usuarioService.existeNombreUsuario("Cosmiq")).isTrue();
    }

    @Test
    void getByNombreUsuario_noExiste_lanzaNoSuchElementConElNombre() {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.getByNombreUsuario("fantasma"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("fantasma");
    }

    @Test
    void getByNombreUsuario_existe_devuelveElUsuario() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cosmiq")).thenReturn(Optional.of(usuario));

        assertThat(usuarioService.getByNombreUsuario("cosmiq")).isEqualTo(usuario);
    }

    // ---------------------------------------------------------------
    // actualizarUltimaConexion
    // ---------------------------------------------------------------

    @Test
    void actualizarUltimaConexion_usuarioExistente_seteaFechaYGuarda() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Cosmiq")).thenReturn(Optional.of(usuario));

        usuarioService.actualizarUltimaConexion("Cosmiq");

        assertThat(usuario.getFechaUltimaConexion()).isNotNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void actualizarUltimaConexion_usuarioInexistente_noGuarda() {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("fantasma")).thenReturn(Optional.empty());

        usuarioService.actualizarUltimaConexion("fantasma");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // ---------------------------------------------------------------
    // actualizarPerfil
    // ---------------------------------------------------------------

    @Test
    void actualizarPerfil_idInexistente_lanzaNoSuchElement() {
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.actualizarPerfil(99L, new ActualizarPerfilDTO()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Usuario no encontrado: 99");
    }

    @Test
    void actualizarPerfil_soloCamposNoNulos_noPisaValoresExistentes() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        usuario.setNombreDisplay("Display original");
        usuario.setBiografia("Bio vieja");
        usuario.setTelefono("111111");
        usuario.setSexo("M");

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO();
        dto.setNombreDisplay("Display nuevo");
        dto.setTelefono("222222");
        // biografia, sexo, pais, banner, preferenciaSensible quedan en null

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = usuarioService.actualizarPerfil(1L, dto);

        assertThat(resultado.getNombreDisplay()).isEqualTo("Display nuevo");
        assertThat(resultado.getTelefono()).isEqualTo("222222");
        assertThat(resultado.getBiografia()).isEqualTo("Bio vieja");
        assertThat(resultado.getSexo()).isEqualTo("M");
        assertThat(usuario.getPreferenciaSensible()).isEqualTo(EPreferenciaContenidoSensible.ADVERTIR);
    }

    @Test
    void actualizarPerfil_conPreferenciaSensible_laActualiza() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        assertThat(usuario.getPreferenciaSensible()).isEqualTo(EPreferenciaContenidoSensible.ADVERTIR);

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO();
        dto.setPreferenciaSensible(EPreferenciaContenidoSensible.OCULTAR);

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = usuarioService.actualizarPerfil(1L, dto);

        assertThat(resultado.getPreferenciaSensible()).isEqualTo(EPreferenciaContenidoSensible.OCULTAR);
    }

    @Test
    void actualizarPerfil_todosLosCamposNoNulos_actualizaCadaUno() {
        Usuario usuario = usuarioDePrueba("Cosmiq");

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO();
        dto.setNombreDisplay("Display nuevo");
        dto.setBiografia("Bio nueva");
        dto.setUrlFotoPerfil("nueva-pfp.png");
        dto.setUrlBanner("nuevo-banner.png");
        dto.setSexo("F");
        dto.setTelefono("333333");
        dto.setPais("Argentina");
        dto.setPreferenciaSensible(EPreferenciaContenidoSensible.OCULTAR);

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = usuarioService.actualizarPerfil(1L, dto);

        assertThat(resultado.getNombreDisplay()).isEqualTo("Display nuevo");
        assertThat(resultado.getBiografia()).isEqualTo("Bio nueva");
        assertThat(resultado.getUrlFotoPerfil()).isEqualTo("nueva-pfp.png");
        assertThat(resultado.getUrlBanner()).isEqualTo("nuevo-banner.png");
        assertThat(resultado.getSexo()).isEqualTo("F");
        assertThat(resultado.getTelefono()).isEqualTo("333333");
        assertThat(resultado.getPais()).isEqualTo("Argentina");
        assertThat(resultado.getPreferenciaSensible()).isEqualTo(EPreferenciaContenidoSensible.OCULTAR);
        verify(usuarioRepository).save(usuario);
    }

    // ---------------------------------------------------------------
    // obtenerTodosLosUsuarios
    // ---------------------------------------------------------------

    @Test
    void obtenerTodosLosUsuarios_mapeaListaADtos() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuarioDePrueba("Cosmiq"), usuarioDePrueba("Artista777")));

        List<UsuarioDTO> resultado = usuarioService.obtenerTodosLosUsuarios();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(UsuarioDTO::getNombreUsuario)
                .containsExactly("Cosmiq", "Artista777");
    }

    // ---------------------------------------------------------------
    // obtenerUsuariosPaginados / aPatronBusqueda (regresión del bug
    // de PostgreSQL: los patrones NUNCA deben viajar vacíos o crudos)
    // ---------------------------------------------------------------

    @Test
    void obtenerUsuariosPaginados_filtrosVacios_pasaPatronesNullAlRepositorio() {
        Pageable pageable = PageRequest.of(0, 10);
        when(usuarioRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<UsuarioDTO> resultado = usuarioService.obtenerUsuariosPaginados(
                null, "   ", null, null, pageable);

        assertThat(resultado.getContent()).isEmpty();
        verify(usuarioRepository).findByFilters(null, null, null, null, pageable);
    }

    @Test
    void obtenerUsuariosPaginados_conTexto_construyePatronLikeEnMinusculas() {
        Pageable pageable = PageRequest.of(0, 10);
        when(usuarioRepository.findByFilters(
                eq("%artista777%"), eq("%correo@mail.com%"), eq(true), eq(ERol.ARTISTA), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(usuarioDePrueba("Artista777")), pageable, 1));

        Page<UsuarioDTO> resultado = usuarioService.obtenerUsuariosPaginados(
                "  Artista777  ", "Correo@MAIL.com", true, ERol.ARTISTA, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNombreUsuario()).isEqualTo("Artista777");
        assertThat(resultado.getContent().get(0).getRol()).isEqualTo("CLIENTE");
    }

    @Test
    void obtenerUsuariosPaginados_conSoloNombre_filtrarCorreoYPasarloNull() {
        Pageable pageable = PageRequest.of(0, 10);
        when(usuarioRepository.findByFilters(
                eq("%cosmiq%"), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(usuarioDePrueba("Cosmiq")), pageable, 1));

        Page<UsuarioDTO> resultado = usuarioService.obtenerUsuariosPaginados(
                "Cosmiq", null, null, null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private RegistroUsuarioDTO registroConRol(ERol rol) {
        RegistroUsuarioDTO dto = new RegistroUsuarioDTO();
        dto.setNombreUsuario("cosmiq");
        dto.setCorreo("cosmiq@mail.com");
        dto.setContrasenia("secreta123");
        dto.setConfirmarContrasenia("secreta123");
        dto.setAcceptaTOS(true);
        dto.setRol(rol);
        return dto;
    }

    private Usuario usuarioDePrueba(String nombreUsuario) {
        Rol rol = Rol.builder().idRol(3L).nombre(ERol.CLIENTE).build();
        return Usuario.builder()
                .idUsuario(1L)
                .nombreUsuario(nombreUsuario)
                .correo(nombreUsuario.toLowerCase() + "@mail.com")
                .contrasenia("$2a$10$hasheado")
                .estadoCuenta(true)
                .fechaCreacion(LocalDateTime.of(2026, 1, 1, 10, 0))
                .rol(rol)
                .build();
    }
}
