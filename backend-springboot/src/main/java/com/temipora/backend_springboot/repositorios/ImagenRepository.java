package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.Imagen;

public interface ImagenRepository extends JpaRepository<Imagen,Long>{
}