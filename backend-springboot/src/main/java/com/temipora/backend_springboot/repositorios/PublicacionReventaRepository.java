package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoPublicacionReventa;
import com.temipora.backend_springboot.modelo.PublicacionReventa;

import jakarta.persistence.LockModeType;

public interface PublicacionReventaRepository
        extends JpaRepository<PublicacionReventa, Long> {

    Optional<PublicacionReventa> findByIdPublicacionReventa(Long idPublicacionReventa);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PublicacionReventa p WHERE p.idPublicacionReventa = :idPublicacionReventa")
    Optional<PublicacionReventa> findByIdPublicacionReventaForUpdate(
            @Param("idPublicacionReventa") Long idPublicacionReventa);

    boolean existsByVarianteIdVarianteAndEstadoIn(
            Long idVariante, Collection<EstadoPublicacionReventa> estados);

    Optional<PublicacionReventa> findFirstByVarianteIdVarianteAndEstadoOrderByFechaPublicacionDesc(
            Long idVariante, EstadoPublicacionReventa estado);

    List<PublicacionReventa> findByVendedorIdUsuarioOrderByFechaPublicacionDesc(Long idUsuario);

    @EntityGraph(attributePaths = {
        "variante",
        "variante.producto",
        "variante.producto.artista",
        "variante.producto.moneda",
        "vendedor",
        "compraItemOrigen"
    })
    List<PublicacionReventa> findByEstadoOrderByFechaPublicacionDesc(
            EstadoPublicacionReventa estado);

    List<PublicacionReventa> findByVarianteIdVarianteInAndEstado(
            Collection<Long> idsVariante, EstadoPublicacionReventa estado);

    Optional<PublicacionReventa> findFirstByVarianteIdVarianteOrderByFechaPublicacionDesc(
            Long idVariante);
}
