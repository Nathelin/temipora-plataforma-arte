package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.Moneda;

public interface MonedaRepository extends JpaRepository<Moneda, Long> {

    Optional<Moneda> findByIdMoneda(Long idMoneda);

    List<Moneda> findAllByOrderByCodigoAsc();

    List<Moneda> findByEstadoTrueOrderByCodigoAsc();

    Optional<Moneda> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdMonedaNot(String codigo, Long idMoneda);

    Optional<Moneda> findFirstByPredeterminadaTrueAndEstadoTrue();

    Optional<Moneda> findFirstByEstadoTrueOrderByIdMonedaAsc();

    Optional<Moneda> findFirstByOrderByIdMonedaAsc();

    long countByEstadoTrue();
}
