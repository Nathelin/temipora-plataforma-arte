package com.temipora.backend_springboot.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Rol;

public interface RolRepository extends JpaRepository<Rol,Long>{

    Optional <Rol> findByNombre(ERol nombre);
    
}
