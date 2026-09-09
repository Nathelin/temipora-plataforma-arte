package com.temipora.backend_springboot.servicios;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.ActualizarPerfilDTO;
import com.temipora.backend_springboot.modelo.RegistroUsuarioDTO;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.repositorios.RolRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class UsuarioService{

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            RolRepository rolRepository) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
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

        return UsuarioDTO.desdeUsuario(usuarioRepository.save(usuario));
    }

    public List<UsuarioDTO> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll().stream()
            .map(UsuarioDTO::desdeUsuario)
            .toList();
    }
}
