package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;

public interface EtiquetaRepository extends JpaRepository<Etiqueta, Long> {

    List<Etiqueta> findAllByOrderByNombreAsc();

    Optional<Etiqueta> findByIdEtiqueta(Long idEtiqueta);

    Optional<Etiqueta> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndEstado(String nombre, EtiquetaEstado estado);

    boolean existsByNombreIgnoreCaseAndEstadoAndIdEtiquetaNot(String nombre, EtiquetaEstado estado, Long idEtiqueta);

    List<Etiqueta> findByEstadoOrderByNombreAsc(EtiquetaEstado estado);

    List<Etiqueta> findByEstadoOrderByFechaCreacionAsc(EtiquetaEstado estado);

    List<Etiqueta> findByEstadoOrderByVecesSolicitadaDesc(EtiquetaEstado estado);

    List<Etiqueta> findByEstadoAndCreadoPorIdUsuario(EtiquetaEstado estado, Long idUsuario);

    List<Etiqueta> findByCreadoPorIdUsuarioOrderByFechaCreacionDesc(Long idUsuario);

    List<Etiqueta> findByEstadoAndCategoriaIdCategoriaEtiquetaOrderByNombreAsc(EtiquetaEstado estado, Long idCategoria);

    List<Etiqueta> findByCategoriaIdCategoriaEtiquetaOrderByNombreAsc(Long idCategoria);

    long countByCategoriaIdCategoriaEtiquetaAndEstadoIn(Long idCategoria, Collection<EtiquetaEstado> estados);
}
