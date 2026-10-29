package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ServicioImagen;

public interface ServicioImagenRepository extends JpaRepository<ServicioImagen, Long> {

    boolean existsByServicioIdServicioAndUrl(Long idServicio, String url);

    void deleteByUrl(String url);
}
