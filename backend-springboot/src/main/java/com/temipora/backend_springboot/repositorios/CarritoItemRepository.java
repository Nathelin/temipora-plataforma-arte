package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.CarritoItem;

public interface CarritoItemRepository extends JpaRepository<CarritoItem, Long> {

    List<CarritoItem> findByCarritoIdCarritoOrderByFechaAgregadoAsc(Long idCarrito);

    Optional<CarritoItem> findByIdCarritoItemAndCarritoIdCarrito(
            Long idCarritoItem, Long idCarrito);

    Optional<CarritoItem> findByCarritoIdCarritoAndVarianteIdVariante(
            Long idCarrito, Long idVariante);

    void deleteByCarritoIdCarrito(Long idCarrito);
}
