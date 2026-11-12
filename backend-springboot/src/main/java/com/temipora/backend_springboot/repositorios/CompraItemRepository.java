package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.CompraItem;

public interface CompraItemRepository extends JpaRepository<CompraItem, Long> {

    List<CompraItem> findByGrupoIdCompraGrupo(Long idCompraGrupo);

    @Query("""
        SELECT i FROM CompraItem i
        JOIN FETCH i.producto
        JOIN FETCH i.variante
        WHERE i.grupo.idCompraGrupo IN :ids
        """)
    List<CompraItem> buscarPorGrupos(@Param("ids") Collection<Long> ids);

    List<CompraItem> findByGrupoCompraIdCompra(Long idCompra);

    Optional<CompraItem> findByIdCompraItemAndGrupoCompraClienteIdUsuario(
            Long idCompraItem, Long idUsuario);
}
