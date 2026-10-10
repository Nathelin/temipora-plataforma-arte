package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ImagenMaterial;

public interface ImagenMaterialRepository extends JpaRepository<ImagenMaterial, Long> {

    List<ImagenMaterial> findByImagenIdImagen(Long idImagen);

    List<ImagenMaterial> findByImagenIdImagenIn(Collection<Long> idsImagen);

    void deleteByImagenIdImagen(Long idImagen);
}
