package com.temipora.backend_springboot.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration 
public class UsuariosConfig {

    @Bean 
    CommandLineRunner crearIndexNombreUsuario(JdbcTemplate jdbcTemplate) {
        return args -> {

            String sql = """
                    CREATE UNIQUE INDEX IF NOT EXISTS uq_usuario_nombre_usuario_lower
                    ON usuario (LOWER(nombre_usuario));
                    """;

            jdbcTemplate.execute(sql);

            System.out.println("Índice de nombre de usuario creado/verificado correctamente.");
        };
    }
}

