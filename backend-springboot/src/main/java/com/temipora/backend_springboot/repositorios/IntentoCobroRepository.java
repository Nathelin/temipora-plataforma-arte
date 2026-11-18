package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobro;

import jakarta.persistence.LockModeType;

public interface IntentoCobroRepository extends JpaRepository<IntentoCobro, Long> {

    Optional<IntentoCobro> findByExternalReference(String externalReference);

    Optional<IntentoCobro> findByClaveIdempotencia(String claveIdempotencia);

    List<IntentoCobro> findByCompraIdCompraOrderByFechaCreacionDesc(Long idCompra);

    Optional<IntentoCobro> findFirstByCompraIdCompraAndEstadoInOrderByFechaCreacionDesc(
            Long idCompra, Collection<EstadoIntentoCobro> estados);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM IntentoCobro i WHERE i.idIntentoCobro = :id")
    Optional<IntentoCobro> findByIdIntentoCobroForUpdate(
            @Param("id") Long id);
}
