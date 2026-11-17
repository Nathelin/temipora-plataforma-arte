package com.temipora.backend_springboot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.ConfiguracionBajaCuentaService;

@ExtendWith(MockitoExtension.class)
class DetallesUsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SancionCuentaRepository sancionCuentaRepository;

    @Mock
    private ConfiguracionBajaCuentaService configuracionBajaCuentaService;

    @Mock
    private ContactoPlataforma contacto;

    @InjectMocks
    private DetallesUsuarioService detallesUsuarioService;

    @Test
    void loadUserByUsername_rolCliente_authorityConPrefijoRole() {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cosmiq"))
                .thenReturn(Optional.of(usuarioConRol("cosmiq", ERol.CLIENTE)));

        UserDetails detalles = detallesUsuarioService.loadUserByUsername("cosmiq");

        assertThat(detalles.getUsername()).isEqualTo("cosmiq");
        assertThat(detalles.getPassword()).isEqualTo("$2a$10$hasheado");
        assertThat(detalles.isEnabled()).isTrue();
        assertThat(detalles.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CLIENTE");
    }

    @Test
    void loadUserByUsername_rolAdmin_expandeJerarquia() {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Cosmiq"))
                .thenReturn(Optional.of(usuarioConRol("Cosmiq", ERol.ADMIN)));

        UserDetails detalles = detallesUsuarioService.loadUserByUsername("Cosmiq");

        assertThat(detalles.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN", "ROLE_ARTISTA", "ROLE_CLIENTE");
    }

    @Test
    void loadUserByUsername_rolArtista_expandeJerarquia() {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Artista777"))
                .thenReturn(Optional.of(usuarioConRol("Artista777", ERol.ARTISTA)));

        UserDetails detalles = detallesUsuarioService.loadUserByUsername("Artista777");

        assertThat(detalles.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ARTISTA", "ROLE_CLIENTE");
    }

    @Test
    void loadUserByUsername_usuarioInexistente_lanzaUsernameNotFound() {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> detallesUsuarioService.loadUserByUsername("fantasma"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("No se encontró al usuario.");
    }

    private Usuario usuarioConRol(String nombreUsuario, ERol rol) {
        Rol cliente = Rol.builder().idRol(3L).nombre(ERol.CLIENTE).build();
        Rol artista = Rol.builder().idRol(2L).nombre(ERol.ARTISTA).rolPadre(cliente).build();
        Rol admin = Rol.builder().idRol(1L).nombre(ERol.ADMIN).rolPadre(artista).build();
        Rol asignado = switch (rol) {
            case ADMIN -> admin;
            case ARTISTA -> artista;
            case CLIENTE -> cliente;
        };

        return Usuario.builder()
                .idUsuario(1L)
                .nombreUsuario(nombreUsuario)
                .correo(nombreUsuario + "@mail.com")
                .contrasenia("$2a$10$hasheado")
                .estadoCuenta(true)
                .rol(asignado)
                .build();
    }
}
