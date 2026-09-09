package com.temipora.backend_springboot.config;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.repositorios.RolRepository;

@Component
@Order(1)
public class RolSeeder implements CommandLineRunner {

    private final RolRepository rolRepository;

    public RolSeeder(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    public void run(String... args) {
        Arrays.stream(ERol.values()).forEach(this::crearSiNoExiste);
    }

    private void crearSiNoExiste(ERol nombreRol) {
        if (rolRepository.findByNombre(nombreRol).isEmpty()) {
            Rol rol = Rol.builder()
                .nombre(nombreRol)
                .estado(true)
                .build();
            rolRepository.save(rol);
            System.out.println("RolSeeder: rol " + nombreRol + " creado.");
        }
    }
}