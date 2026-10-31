package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EstadoConversacion;
import com.temipora.backend_springboot.modelo.EstadoMensaje;
import com.temipora.backend_springboot.modelo.Mensaje;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    @Query("""
        SELECT m FROM Mensaje m
        JOIN FETCH m.remitente
        WHERE m.conversacion.idConversacion = :idConversacion
          AND m.estado = :estado
          AND (:antesDe IS NULL OR m.idMensaje < :antesDe)
        ORDER BY m.idMensaje DESC
        """)
    List<Mensaje> listarHistorial(
            @Param("idConversacion") Long idConversacion,
            @Param("antesDe") Long antesDe,
            @Param("estado") EstadoMensaje estado,
            Pageable pageable);

    @Query("""
        SELECT m FROM Mensaje m
        WHERE m.idMensaje IN (
            SELECT MAX(m2.idMensaje)
            FROM Mensaje m2
            WHERE m2.conversacion.idConversacion IN :ids
              AND m2.estado = :estado
            GROUP BY m2.conversacion.idConversacion
        )
        """)
    List<Mensaje> ultimosMensajes(
            @Param("ids") Collection<Long> ids,
            @Param("estado") EstadoMensaje estado);

    @Query("""
        SELECT COUNT(m)
        FROM Mensaje m, ConversacionParticipante p
        WHERE p.conversacion.idConversacion = m.conversacion.idConversacion
          AND p.usuario.idUsuario = :idUsuario
          AND m.remitente.idUsuario <> :idUsuario
          AND m.estado = :estadoMensaje
          AND m.conversacion.estado = :estadoConversacion
          AND (p.ultimoLeidoEn IS NULL OR m.fechaEnvio > p.ultimoLeidoEn)
        """)
    long contarNoLeidos(
            @Param("idUsuario") Long idUsuario,
            @Param("estadoMensaje") EstadoMensaje estadoMensaje,
            @Param("estadoConversacion") EstadoConversacion estadoConversacion);

    @Query("""
        SELECT m.conversacion.idConversacion, COUNT(m)
        FROM Mensaje m, ConversacionParticipante p
        WHERE p.conversacion.idConversacion = m.conversacion.idConversacion
          AND p.usuario.idUsuario = :idUsuario
          AND m.conversacion.idConversacion IN :ids
          AND m.remitente.idUsuario <> :idUsuario
          AND m.estado = :estadoMensaje
          AND (p.ultimoLeidoEn IS NULL OR m.fechaEnvio > p.ultimoLeidoEn)
        GROUP BY m.conversacion.idConversacion
        """)
    List<Object[]> contarNoLeidosPorConversacion(
            @Param("idUsuario") Long idUsuario,
            @Param("ids") Collection<Long> ids,
            @Param("estadoMensaje") EstadoMensaje estadoMensaje);

    long countByRemitenteIdUsuarioAndFechaEnvioAfter(
            Long idRemitente, LocalDateTime desde);
}
