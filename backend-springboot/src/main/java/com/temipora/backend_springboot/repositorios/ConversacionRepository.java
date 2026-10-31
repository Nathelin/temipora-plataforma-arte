package com.temipora.backend_springboot.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.Conversacion;
import com.temipora.backend_springboot.modelo.EstadoConversacion;

import jakarta.persistence.LockModeType;

public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    Optional<Conversacion> findByIdConversacion(Long idConversacion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conversacion c WHERE c.idConversacion = :idConversacion")
    Optional<Conversacion> findByIdConversacionForUpdate(
            @Param("idConversacion") Long idConversacion);

    @Query("""
        SELECT c FROM Conversacion c
        WHERE (c.iniciador.idUsuario = :idUsuarioA AND c.destinatario.idUsuario = :idUsuarioB)
           OR (c.iniciador.idUsuario = :idUsuarioB AND c.destinatario.idUsuario = :idUsuarioA)
        """)
    Optional<Conversacion> buscarPorPar(
            @Param("idUsuarioA") Long idUsuarioA,
            @Param("idUsuarioB") Long idUsuarioB);

    long countByIniciadorIdUsuarioAndEstado(
            Long idIniciador, EstadoConversacion estado);

    long countByDestinatarioIdUsuarioAndEstado(
            Long idDestinatario, EstadoConversacion estado);
}
