package com.temipora.backend_springboot.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void run(String... args) {
        // La jerarquía se crea de abajo hacia arriba para poder referenciar al padre.
        Rol cliente = crearSiNoExiste(ERol.CLIENTE, null);
        Rol artista = crearSiNoExiste(ERol.ARTISTA, cliente);
        crearSiNoExiste(ERol.ADMIN, artista);
    }

    private Rol crearSiNoExiste(ERol nombreRol, Rol rolPadre) {
        Rol rol = rolRepository.findByNombre(nombreRol).orElseGet(() -> {
            Rol nuevo = Rol.builder()
                .nombre(nombreRol)
                .estado(true)
                .rolPadre(rolPadre)
                .build();
            System.out.println("RolSeeder: rol " + nombreRol + " creado.");
            return rolRepository.save(nuevo);
        });

        boolean padreDesactualizado = rolPadre != null
            && (rol.getRolPadre() == null
                || !rolPadre.getIdRol().equals(rol.getRolPadre().getIdRol()));
        if (padreDesactualizado) {
            rol.setRolPadre(rolPadre);
            rol = rolRepository.save(rol);
            System.out.println("RolSeeder: jerarquía de " + nombreRol + " actualizada.");
        }
        return rol;
    }
}