package com.temipora.backend_springboot.servicios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.ActualizarContraseniaDTO;
import com.temipora.backend_springboot.modelo.ActualizarCorreoDTO;
import com.temipora.backend_springboot.modelo.ActualizarPerfilDTO;
import com.temipora.backend_springboot.modelo.ActualizarTelefonoDTO;
import com.temipora.backend_springboot.modelo.Pais;
import com.temipora.backend_springboot.modelo.QuitarTelefonoDTO;
import com.temipora.backend_springboot.modelo.RegistroUsuarioDTO;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.repositorios.PaisRepository;
import com.temipora.backend_springboot.repositorios.RolRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;
    private final PaisRepository paisRepository;
    private final TelefonoService telefonoService;
    private final MonedaService monedaService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            RolRepository rolRepository,
            PaisRepository paisRepository,
            TelefonoService telefonoService,
            MonedaService monedaService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
        this.paisRepository = paisRepository;
        this.telefonoService = telefonoService;
        this.monedaService = monedaService;
    }

    public Usuario registrarUsuario(RegistroUsuarioDTO dto) {

        if (dto.getRol() == ERol.ADMIN) {
            throw new IllegalArgumentException("No se puede auto-registrar un usuario con rol ADMIN");
        }

        Rol rol = rolRepository
                .findByNombre(dto.getRol())
                .orElseThrow(() -> new IllegalStateException("El rol " + dto.getRol() + " no existe"));

        Usuario usuario = Usuario.builder()
                .nombreUsuario(dto.getNombreUsuario())
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .correo(dto.getCorreo())
                .contrasenia(passwordEncoder.encode(dto.getContrasenia()))
                .fechaNacimiento(dto.getFechaNacimiento())
                .biografia(dto.getBiografia())
                .urlFotoPerfil(dto.getUrlFotoPerfil())
                .estadoCuenta(true)
                .fechaCreacion(LocalDateTime.now())
                .monedaPreferida(monedaService.predeterminada())
                .rol(rol)
                .build();

        return usuarioRepository.save(usuario);
    }

    public boolean existeNombreUsuario(String nombreUsuario) {
        return usuarioRepository.existsByNombreUsuarioIgnoreCase(nombreUsuario);
    }

    public Usuario getByNombreUsuario(String nombreUsuario) {
        return usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario)
                .orElseThrow(() -> new NoSuchElementException(nombreUsuario));
    }

    public void actualizarUltimaConexion(String nombreUsuario) {
        usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario).ifPresent(u -> {
            u.setFechaUltimaConexion(LocalDateTime.now());
            usuarioRepository.save(u);
        });
    }

    public UsuarioDTO actualizarPerfil(Long idUsuario, ActualizarPerfilDTO dto) {
        Usuario usuario = usuarioRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario));

        if (dto.getNombreDisplay() != null) {
            usuario.setNombreDisplay(dto.getNombreDisplay());
        }
        if (dto.getBiografia() != null) {
            usuario.setBiografia(dto.getBiografia());
        }
        if (dto.getUrlFotoPerfil() != null) {
            usuario.setUrlFotoPerfil(dto.getUrlFotoPerfil());
        }
        if (dto.getUrlBanner() != null) {
            usuario.setUrlBanner(dto.getUrlBanner());
        }
        if (dto.getSexo() != null) {
            usuario.setSexo(dto.getSexo());
        }
        if (dto.getIdPais() != null) {
            Pais pais = paisRepository.findByIdPais(dto.getIdPais())
                    .orElseThrow(() -> new IllegalArgumentException("País no encontrado: " + dto.getIdPais()));

            boolean esElMismo = usuario.getPais() != null
                    && usuario.getPais().getIdPais().equals(pais.getIdPais());

            if (!pais.isEstado() && !esElMismo) {
                throw new IllegalArgumentException(
                        "El país '" + pais.getNombre() + "' fue dado de baja y no puede asignarse.");
            }

            usuario.setPais(pais);
        }
        if (dto.getPreferenciaSensible() != null) {
            usuario.setPreferenciaSensible(dto.getPreferenciaSensible());
        }
        if (dto.getIdMonedaPreferida() != null) {
            usuario.setMonedaPreferida(monedaService.obtener(dto.getIdMonedaPreferida()));
        }

        return UsuarioDTO.desdeUsuario(usuarioRepository.save(usuario));
    }

    public List<UsuarioDTO> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioDTO::desdeUsuario)
                .toList();
    }

    public Page<UsuarioDTO> obtenerUsuariosPaginados(String nombreUsuario, String correo, Boolean estadoCuenta, ERol rol, Pageable pageable) {
        return usuarioRepository.findByFilters(
                aPatronBusqueda(nombreUsuario),
                aPatronBusqueda(correo),
                estadoCuenta,
                rol,
                pageable)
                .map(UsuarioDTO::desdeUsuario);
    }

    public String nombreUsuario(Long idUsuario) {
        return usuarioRepository.findByIdUsuario(idUsuario)
                .map(Usuario::getNombreUsuario)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario));
    }

    @Transactional
    public UsuarioDTO cambiarCorreo(Long idUsuario, ActualizarCorreoDTO dto) {
        Usuario usuario = usuarioRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario));

        if (!passwordEncoder.matches(dto.getContraseniaActual(), usuario.getContrasenia())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }

        String nuevoCorreo = dto.getCorreo().trim();

        usuarioRepository.findByCorreo(nuevoCorreo)
                .filter(otro -> !otro.getIdUsuario().equals(idUsuario))
                .ifPresent(otro -> {
                    throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");
                });

        usuario.setCorreo(nuevoCorreo);
        return UsuarioDTO.desdeUsuario(usuarioRepository.save(usuario));
    }

    @Transactional
    public void cambiarContrasenia(Long idUsuario, ActualizarContraseniaDTO dto) {
        Usuario usuario = usuarioRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario));

        if (!passwordEncoder.matches(dto.getContraseniaActual(), usuario.getContrasenia())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        if (!dto.getContraseniaNueva().equals(dto.getConfirmarContrasenia())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        if (passwordEncoder.matches(dto.getContraseniaNueva(), usuario.getContrasenia())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta a la actual.");
        }

        usuario.setContrasenia(passwordEncoder.encode(dto.getContraseniaNueva()));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public UsuarioDTO cambiarTelefono(Long idUsuario, ActualizarTelefonoDTO dto) {
        Usuario usuario = usuarioRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario));

        if (!passwordEncoder.matches(dto.getContraseniaActual(), usuario.getContrasenia())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }

        String codigoIso = usuario.getPais() != null ? usuario.getPais().getCodigoIso() : null;
        String telefonoAnterior = usuario.getTelefono();

        if (telefonoAnterior != null && !telefonoAnterior.isBlank()) {
            if (dto.getTelefonoActual() == null || dto.getTelefonoActual().isBlank()) {
                throw new IllegalArgumentException("Debés ingresar tu teléfono actual.");
            }
            String telefonoActual = normalizarTelefonoActual(dto.getTelefonoActual(), codigoIso);
            if (!telefonoActual.equals(telefonoAnterior)) {
                throw new IllegalArgumentException("El teléfono actual no coincide con el registrado.");
            }
        }

        String telefonoNuevo = telefonoService.validarYNormalizar(dto.getTelefonoNuevo(), codigoIso);

        if (telefonoNuevo.equals(telefonoAnterior)) {
            throw new IllegalArgumentException("El nuevo teléfono debe ser distinto al actual.");
        }

        usuarioRepository.findByTelefono(telefonoNuevo)
                .filter(otro -> !otro.getIdUsuario().equals(idUsuario))
                .ifPresent(otro -> {
                    throw new IllegalArgumentException("Ya existe una cuenta con ese teléfono.");
                });

        usuario.setTelefono(telefonoNuevo);
        return UsuarioDTO.desdeUsuario(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioDTO quitarTelefono(Long idUsuario, QuitarTelefonoDTO dto) {
        Usuario usuario = usuarioRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idUsuario));

        if (!passwordEncoder.matches(dto.getContraseniaActual(), usuario.getContrasenia())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }

        usuario.setTelefono(null);
        return UsuarioDTO.desdeUsuario(usuarioRepository.save(usuario));
    }

    private String normalizarTelefonoActual(String telefono, String codigoIso) {
        String region = telefono.trim().startsWith("+") ? null : codigoIso;
        return telefonoService.validarYNormalizar(telefono, region);
    }

    private String aPatronBusqueda(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return "%" + texto.trim().toLowerCase() + "%";
    }
}