package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.TipoCampo;

public interface TipoCampoRepository extends JpaRepository<TipoCampo, Long> {

    List<TipoCampo> findByActivoTrueOrderByNombreAsc();

    Optional<TipoCampo> findByIdTipoCampo(Long idTipoCampo);

    Optional<TipoCampo> findByCodigo(String codigo);
}
