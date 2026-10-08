package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.Material;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findAllByOrderByNombreAsc();

    List<Material> findByActivoTrueOrderByNombreAsc();

    Optional<Material> findByIdMaterial(Long idMaterial);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdMaterialNot(String nombre, Long idMaterial);

    @Query("""
        SELECT m FROM Material m
        WHERE m.activo = true
          AND m.tipoArte.activo = true
          AND (:idMedio IS NULL OR m.tipoArte.medio.idMedio = :idMedio)
        ORDER BY m.nombre ASC
        """)
    List<Material> buscarSelector(@Param("idMedio") Long idMedio);
}
