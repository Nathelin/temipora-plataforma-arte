package com.temipora.backend_springboot.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.temipora.backend_springboot.modelo.ServicioImagen;

public interface ServicioImagenRepository extends JpaRepository<ServicioImagen, Long> {

    boolean existsByServicioIdServicioAndUrl(Long idServicio, String url);

    boolean existsByUrl(String url);

    @Query("SELECT si.url FROM ServicioImagen si WHERE si.url IS NOT NULL")
    List<String> findUrls();

    void deleteByUrl(String url);
}
