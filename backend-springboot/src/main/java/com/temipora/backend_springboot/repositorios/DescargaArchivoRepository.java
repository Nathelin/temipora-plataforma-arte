package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.DescargaArchivo;

public interface DescargaArchivoRepository
        extends JpaRepository<DescargaArchivo, Long> {
}
