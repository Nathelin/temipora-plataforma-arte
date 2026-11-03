package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;

public interface EtiquetaRepository extends JpaRepository<Etiqueta, Long> {

    Optional<Etiqueta> findByIdEtiqueta(Long idEtiqueta);

    Optional<Etiqueta> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndEstado(String nombre, EtiquetaEstado estado);

    boolean existsByNombreIgnoreCaseAndEstadoAndIdEtiquetaNot(String nombre, EtiquetaEstado estado, Long idEtiqueta);

    Page<Etiqueta> findByEstado(EtiquetaEstado estado, Pageable pageable);

    Page<Etiqueta> findByCategoriaIdCategoriaEtiqueta(Long idCategoria, Pageable pageable);

    Page<Etiqueta> findByEstadoAndCategoriaIdCategoriaEtiqueta(
            EtiquetaEstado estado, Long idCategoria, Pageable pageable);

    long countByEstado(EtiquetaEstado estado);

    List<Etiqueta> findByEstadoOrderByNombreAsc(EtiquetaEstado estado);

    List<Etiqueta> findByEstadoAndCreadoPorIdUsuario(EtiquetaEstado estado, Long idUsuario);

    List<Etiqueta> findByCreadoPorIdUsuarioOrderByFechaCreacionDesc(Long idUsuario);

    long countByCategoriaIdCategoriaEtiquetaAndEstadoIn(Long idCategoria, Collection<EtiquetaEstado> estados);

    @Query("""
        SELECT DISTINCT e FROM ImagenEtiqueta ie
          JOIN ie.etiqueta e
          JOIN ie.imagen i
        WHERE i.portfolio.idPortfolio = :portfolioId
          AND i.estado = true
        ORDER BY e.nombre
        """)
    List<Etiqueta> findEtiquetasDePortfolio(@Param("portfolioId") Long portfolioId);
}
