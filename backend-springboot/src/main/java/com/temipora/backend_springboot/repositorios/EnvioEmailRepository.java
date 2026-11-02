package com.temipora.backend_springboot.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.EnvioEmail;

public interface EnvioEmailRepository extends JpaRepository<EnvioEmail, Long> {

    boolean existsByClaveIdempotencia(String claveIdempotencia);

    Optional<EnvioEmail> findByClaveIdempotencia(String claveIdempotencia);
}
