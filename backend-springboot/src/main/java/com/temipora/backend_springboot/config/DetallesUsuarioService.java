package com.temipora.backend_springboot.config;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
    public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario)
                                     .orElseThrow(()-> new UsernameNotFoundException("No se encontró al usuario."));

        Collection<? extends GrantedAuthority> permisos = java.util.Collections.singleton(
            new SimpleGrantedAuthority("ROLE_".concat(usuario.getRol().getNombre().name())));
         
        return new User(usuario.getNombreUsuario(), usuario.getContrasenia(), true, true, true, true, permisos);

        // el return tiene cuatro booleans, entre ellos para definir permisos y si hay bloqueos o expiración de sesión.
        // TODO: investigar mejor
    }
}