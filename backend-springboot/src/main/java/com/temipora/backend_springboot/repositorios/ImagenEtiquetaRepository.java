package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ImagenEtiqueta;

public interface ImagenEtiquetaRepository extends JpaRepository<ImagenEtiqueta, Long> {

    boolean existsByImagenIdImagenAndEtiquetaIdEtiqueta(Long idImagen, Long idEtiqueta);

    List<ImagenEtiqueta> findByImagenIdImagen(Long idImagen);

    List<ImagenEtiqueta> findByImagenIdImagenIn(Collection<Long> idsImagen);

    void deleteByEtiquetaIdEtiqueta(Long idEtiqueta);
}
