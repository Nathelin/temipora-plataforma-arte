package com.temipora.backend_springboot.config;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.CuentaSancionadaException;

//interfaz que actua como colaborador entre el framework y la info del usuario para autenticar y autorizar
@Service
public class DetallesUsuarioService implements UserDetailsService, AccesoUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SancionCuentaRepository sancionCuentaRepository;
    private final ContactoPlataforma contacto;

    public DetallesUsuarioService(
            UsuarioRepository usuarioRepository,
            SancionCuentaRepository sancionCuentaRepository,
            ContactoPlataforma contacto) {
        this.usuarioRepository = usuarioRepository;
        this.sancionCuentaRepository = sancionCuentaRepository;
        this.contacto = contacto;
    }

    /**
     * Autenticación por contraseña. No evalúa sanciones: el login debe verificar
     * la contraseña antes de revelar el motivo (lo hace JwtAuthenticationFilter).
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario)
                                     .orElseThrow(()-> new UsernameNotFoundException("No se encontró al usuario."));
        return construirDetalles(usuario);
    }

    /**
     * Autorización de una sesión ya emitida (token JWT o WebSocket). Acá sí se
     * bloquea una cuenta desactivada o con sanción vigente.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserForSession(String nombreUsuario) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario)
                                     .orElseThrow(()-> new UsernameNotFoundException("No se encontró al usuario."));

        if (!usuario.isEstadoCuenta()) {
            throw new DisabledException("La cuenta está desactivada.");
        }

        List<SancionCuenta> sancionesVigentes = sancionCuentaRepository
            .findVigentesByUsuarioId(usuario.getIdUsuario(), EstadoSancion.ACTIVA, LocalDateTime.now());
        if (!sancionesVigentes.isEmpty()) {
            SancionCuenta sancion = sancionesVigentes.get(0);
            throw new CuentaSancionadaException(
                sancion.getMotivo(), contacto.getEmail(), sancion.getFechaFin());
        }

        return construirDetalles(usuario);
    }

    private UserDetails construirDetalles(Usuario usuario) {
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
            usuario.isEstadoCuenta(), true, true, true, autoridades);
    }
}
