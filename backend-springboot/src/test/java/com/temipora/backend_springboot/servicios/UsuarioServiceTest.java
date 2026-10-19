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

import com.temipora.backend_springboot.modelo.ActualizarContraseniaDTO;
import com.temipora.backend_springboot.modelo.ActualizarCorreoDTO;
import com.temipora.backend_springboot.modelo.ActualizarPerfilDTO;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Pais;
import com.temipora.backend_springboot.modelo.RegistroUsuarioDTO;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.repositorios.PaisRepository;
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

    @Mock
    private PaisRepository paisRepository;

    @Mock
    private TelefonoService telefonoService;

    @Mock
    private MonedaService monedaService;

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
        when(monedaService.predeterminada()).thenReturn(Moneda.builder().idMoneda(1L).codigo("ARS").build());
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
        when(telefonoService.validarYNormalizar("222222", null)).thenReturn("+222222");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = usuarioService.actualizarPerfil(1L, dto);

        assertThat(resultado.getNombreDisplay()).isEqualTo("Display nuevo");
        assertThat(resultado.getTelefono()).isEqualTo("+222222");
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
        dto.setIdPais(1L);
        dto.setPreferenciaSensible(EPreferenciaContenidoSensible.OCULTAR);

        Pais argentina = Pais.builder().idPais(1L).nombre("Argentina").codigoIso("AR").prefijoTelefonico("+54").estado(true).build();
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(paisRepository.findByIdPais(1L)).thenReturn(Optional.of(argentina));
        when(telefonoService.validarYNormalizar("333333", "AR")).thenReturn("+54333333");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = usuarioService.actualizarPerfil(1L, dto);

        assertThat(resultado.getNombreDisplay()).isEqualTo("Display nuevo");
        assertThat(resultado.getBiografia()).isEqualTo("Bio nueva");
        assertThat(resultado.getUrlFotoPerfil()).isEqualTo("nueva-pfp.png");
        assertThat(resultado.getUrlBanner()).isEqualTo("nuevo-banner.png");
        assertThat(resultado.getSexo()).isEqualTo("F");
        assertThat(resultado.getTelefono()).isEqualTo("+54333333");
        assertThat(resultado.getIdPais()).isEqualTo(1L);
        assertThat(resultado.getNombrePais()).isEqualTo("Argentina");
        assertThat(resultado.getPreferenciaSensible()).isEqualTo(EPreferenciaContenidoSensible.OCULTAR);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void actualizarPerfil_paisDadoDeBajaDistinto_lanzaError() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        Pais inactivo = Pais.builder().idPais(2L).nombre("Narnia").estado(false).build();

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO();
        dto.setIdPais(2L);

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(paisRepository.findByIdPais(2L)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> usuarioService.actualizarPerfil(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El país 'Narnia' fue dado de baja y no puede asignarse.");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarPerfil_conservaSuPaisInactivo_loPermite() {
        Pais inactivo = Pais.builder().idPais(2L).nombre("Narnia").estado(false).build();
        Usuario usuario = usuarioDePrueba("Cosmiq");
        usuario.setPais(inactivo);

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO();
        dto.setIdPais(2L);

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(paisRepository.findByIdPais(2L)).thenReturn(Optional.of(inactivo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = usuarioService.actualizarPerfil(1L, dto);

        assertThat(resultado.getNombrePais()).isEqualTo("Narnia");
    }

    @Test
    void actualizarPerfil_paisInexistente_lanzaError() {
        Usuario usuario = usuarioDePrueba("Cosmiq");

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO();
        dto.setIdPais(99L);

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(paisRepository.findByIdPais(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.actualizarPerfil(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("País no encontrado: 99");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // ---------------------------------------------------------------
    // nombreUsuario / cambiarCorreo
    // ---------------------------------------------------------------

    @Test
    void nombreUsuario_existente_devuelveElNombre() {
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuarioDePrueba("Cosmiq")));

        assertThat(usuarioService.nombreUsuario(1L)).isEqualTo("Cosmiq");
    }

    @Test
    void nombreUsuario_inexistente_lanzaNoSuchElement() {
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.nombreUsuario(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Usuario no encontrado: 99");
    }

    @Test
    void cambiarCorreo_ok_actualizaElCorreo() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        ActualizarCorreoDTO dto = new ActualizarCorreoDTO();
        dto.setCorreo("nuevo@mail.com");
        dto.setContraseniaActual("secreta123");

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secreta123", "$2a$10$hasheado")).thenReturn(true);
        when(usuarioRepository.findByCorreo("nuevo@mail.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = usuarioService.cambiarCorreo(1L, dto);

        assertThat(resultado.getCorreo()).isEqualTo("nuevo@mail.com");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void cambiarCorreo_contraseniaIncorrecta_lanzaError() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        ActualizarCorreoDTO dto = new ActualizarCorreoDTO();
        dto.setCorreo("nuevo@mail.com");
        dto.setContraseniaActual("mala");

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "$2a$10$hasheado")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.cambiarCorreo(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La contraseña actual no es correcta.");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void cambiarCorreo_correoDeOtroUsuario_lanzaError() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        Usuario otro = usuarioDePrueba("Artista777");
        otro.setIdUsuario(2L);
        otro.setCorreo("ocupado@mail.com");

        ActualizarCorreoDTO dto = new ActualizarCorreoDTO();
        dto.setCorreo("ocupado@mail.com");
        dto.setContraseniaActual("secreta123");

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secreta123", "$2a$10$hasheado")).thenReturn(true);
        when(usuarioRepository.findByCorreo("ocupado@mail.com")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> usuarioService.cambiarCorreo(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una cuenta con ese correo.");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void cambiarCorreo_mismoCorreoPropio_loPermite() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        ActualizarCorreoDTO dto = new ActualizarCorreoDTO();
        dto.setCorreo("cosmiq@mail.com");
        dto.setContraseniaActual("secreta123");

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secreta123", "$2a$10$hasheado")).thenReturn(true);
        when(usuarioRepository.findByCorreo("cosmiq@mail.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = usuarioService.cambiarCorreo(1L, dto);

        assertThat(resultado.getCorreo()).isEqualTo("cosmiq@mail.com");
    }

    // ---------------------------------------------------------------
    // cambiarContrasenia
    // ---------------------------------------------------------------

    @Test
    void cambiarContrasenia_ok_actualizaHasheando() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        ActualizarContraseniaDTO dto = new ActualizarContraseniaDTO();
        dto.setContraseniaActual("Actual123");
        dto.setContraseniaNueva("Nueva123");
        dto.setConfirmarContrasenia("Nueva123");

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Actual123", "$2a$10$hasheado")).thenReturn(true);
        when(passwordEncoder.matches("Nueva123", "$2a$10$hasheado")).thenReturn(false);
        when(passwordEncoder.encode("Nueva123")).thenReturn("$2a$10$nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.cambiarContrasenia(1L, dto);

        assertThat(usuario.getContrasenia()).isEqualTo("$2a$10$nuevo");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void cambiarContrasenia_actualIncorrecta_lanzaError() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        ActualizarContraseniaDTO dto = new ActualizarContraseniaDTO();
        dto.setContraseniaActual("mala");
        dto.setContraseniaNueva("Nueva123");
        dto.setConfirmarContrasenia("Nueva123");

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "$2a$10$hasheado")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.cambiarContrasenia(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La contraseña actual no es correcta.");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void cambiarContrasenia_noCoinciden_lanzaError() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        ActualizarContraseniaDTO dto = new ActualizarContraseniaDTO();
        dto.setContraseniaActual("Actual123");
        dto.setContraseniaNueva("Nueva123");
        dto.setConfirmarContrasenia("Distinta123");

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Actual123", "$2a$10$hasheado")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cambiarContrasenia(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Las contraseñas no coinciden.");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void cambiarContrasenia_igualALaActual_lanzaError() {
        Usuario usuario = usuarioDePrueba("Cosmiq");
        ActualizarContraseniaDTO dto = new ActualizarContraseniaDTO();
        dto.setContraseniaActual("Actual123");
        dto.setContraseniaNueva("Actual123");
        dto.setConfirmarContrasenia("Actual123");

        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Actual123", "$2a$10$hasheado")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cambiarContrasenia(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La nueva contraseña debe ser distinta a la actual.");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void cambiarContrasenia_usuarioInexistente_lanzaNoSuchElement() {
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.empty());

        ActualizarContraseniaDTO dto = new ActualizarContraseniaDTO();
        dto.setContraseniaActual("Actual123");
        dto.setContraseniaNueva("Nueva123");
        dto.setConfirmarContrasenia("Nueva123");

        assertThatThrownBy(() -> usuarioService.cambiarContrasenia(99L, dto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Usuario no encontrado: 99");
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
