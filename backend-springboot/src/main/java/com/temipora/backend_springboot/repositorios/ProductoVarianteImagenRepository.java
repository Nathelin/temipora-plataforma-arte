package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ProductoVarianteImagen;

public interface ProductoVarianteImagenRepository
        extends JpaRepository<ProductoVarianteImagen, Long> {

    List<ProductoVarianteImagen> findByVarianteIdVarianteOrderByNumeroDesc(Long idVariante);

    Optional<ProductoVarianteImagen> findTopByVarianteIdVarianteOrderByNumeroDesc(
            Long idVariante);

    Optional<ProductoVarianteImagen> findByVarianteIdVarianteAndVigenteTrue(
            Long idVariante);

    Optional<ProductoVarianteImagen>
        findFirstByVarianteIdVarianteAndSha256OrderByNumeroDesc(
            Long idVariante, String sha256);
}
