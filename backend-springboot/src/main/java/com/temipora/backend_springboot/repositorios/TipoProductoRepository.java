package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.TipoProducto;

public interface TipoProductoRepository extends JpaRepository<TipoProducto, Long> {

    List<TipoProducto> findAllByOrderByNombreAsc();

    Optional<TipoProducto> findByIdTipoProducto(Long idTipoProducto);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdTipoProductoNot(String nombre, Long idTipoProducto);
}