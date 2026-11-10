package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ProductoEntregable;

public interface ProductoEntregableRepository
        extends JpaRepository<ProductoEntregable, Long> {

    List<ProductoEntregable> findByProductoIdProductoOrderByOrdenAsc(Long idProducto);

    List<ProductoEntregable> findByProductoIdProductoInOrderByOrdenAsc(
            Collection<Long> idsProducto);

    void deleteByProductoIdProducto(Long idProducto);

    boolean existsByVarianteIdVariante(Long idVariante);
}
