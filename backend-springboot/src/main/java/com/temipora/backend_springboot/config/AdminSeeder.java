package com.temipora.backend_springboot.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.RolRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        boolean yaExisteAdmin = usuarioRepository.existsByRol_Nombre(ERol.ADMIN);
        if (yaExisteAdmin) {
            return; // ya está
        }

        String correo = System.getenv("ADMIN_CORREO");
        String contrasenia = System.getenv("ADMIN_CONTRASENIA");
        String nombreUsuario = System.getenv("ADMIN_NOMBRE_USUARIO");

        if (correo == null || contrasenia == null || nombreUsuario == null) {
            // sin variables de entorno, no creamos nada (evita admin con clave default)
            System.out.println("AdminSeeder: variables de entorno no definidas, se omite creación de admin.");
            return;
        }

        Rol rolAdmin = rolRepository.findByNombre(ERol.ADMIN)
            .orElseThrow(() -> new IllegalStateException("Rol ADMIN no existe en la base de datos"));

        Usuario admin = Usuario.builder()
            .nombreUsuario(nombreUsuario)
            .correo(correo)
            .contrasenia(passwordEncoder.encode(contrasenia))
            .estadoCuenta(true)
            .fechaCreacion(LocalDateTime.now())
            .rol(rolAdmin)
            .build();

        usuarioRepository.save(admin);
        System.out.println("Admin bootstrapeado: " + correo);
    }
}