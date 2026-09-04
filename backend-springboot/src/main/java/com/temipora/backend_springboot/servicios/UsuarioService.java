package com.temipora.backend_springboot.servicios;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.RegistroUsuarioDTO;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
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

        Rol rolCliente = rolRepository
            .findByNombre(ERol.CLIENTE)
            .orElseThrow();

        Usuario usuario = Usuario.builder()
            //.nombre(dto.getNombre())
            //.apellido(dto.getApellido())
            .nombreUsuario(dto.getNombreUsuario())
            .correo(dto.getCorreo())
            .contrasenia(passwordEncoder.encode(dto.getContrasenia()))
            .estadoCuenta(true)
            .fechaCreacion(LocalDateTime.now())
            .rol(rolCliente)
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
}
