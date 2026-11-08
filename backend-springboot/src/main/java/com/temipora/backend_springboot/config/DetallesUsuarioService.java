package com.temipora.backend_springboot.config;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

//interfaz que actua como colaborador entre el framework y la info del usuario para autenticar y autorizar
@Service
public class DetallesUsuarioService implements UserDetailsService{

    private final UsuarioRepository usuarioRepository; 

    public DetallesUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario)
                                     .orElseThrow(()-> new UsernameNotFoundException("No se encontró al usuario."));

        // El usuario tiene un solo rol, pero hereda las autoridades de sus ancestros:
        // CLIENTE <- ARTISTA <- ADMIN. Se devuelve el rol principal primero.
        Set<GrantedAuthority> autoridades = new LinkedHashSet<>();
        Set<Long> visitados = new LinkedHashSet<>();
        Rol rol = usuario.getRol();
        while (rol != null && visitados.add(rol.getIdRol())) {
            if (rol.getNombre() != null) {
                autoridades.add(new SimpleGrantedAuthority(
                    "ROLE_".concat(rol.getNombre().name())));
            }
            rol = rol.getRolPadre();
        }

        return new User(usuario.getNombreUsuario(), usuario.getContrasenia(),
            true, true, true, true, autoridades);

        // el return tiene cuatro booleans, entre ellos para definir permisos y si hay bloqueos o expiración de sesión.
        // TODO: investigar mejor
    }
}
