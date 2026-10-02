package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.CategoriaEtiqueta;

public interface CategoriaEtiquetaRepository extends JpaRepository<CategoriaEtiqueta, Long> {

    List<CategoriaEtiqueta> findAllByOrderByNombreAsc();

    List<CategoriaEtiqueta> findByActivoTrueOrderByNombreAsc();

    Optional<CategoriaEtiqueta> findByIdCategoriaEtiqueta(Long idCategoriaEtiqueta);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdCategoriaEtiquetaNot(String nombre, Long idCategoriaEtiqueta);
}
