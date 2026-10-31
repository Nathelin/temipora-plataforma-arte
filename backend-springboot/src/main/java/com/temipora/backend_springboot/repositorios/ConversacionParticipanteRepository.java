package com.temipora.backend_springboot.repositorios;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.ConversacionParticipante;
import com.temipora.backend_springboot.modelo.EstadoConversacion;

public interface ConversacionParticipanteRepository
        extends JpaRepository<ConversacionParticipante, Long> {

    Optional<ConversacionParticipante> findByConversacionIdConversacionAndUsuarioIdUsuario(
            Long idConversacion, Long idUsuario);

    Optional<ConversacionParticipante>
        findByConversacionIdConversacionAndUsuarioIdUsuarioNot(
            Long idConversacion, Long idUsuario);

    @Query(value = """
        SELECT p
        FROM ConversacionParticipante p
        JOIN FETCH p.conversacion c
        JOIN FETCH c.iniciador
        JOIN FETCH c.destinatario
        WHERE p.usuario.idUsuario = :idUsuario
          AND p.archivada = :archivada
          AND c.estado IN :estados
          AND (:soloRecibidas = false OR c.destinatario.idUsuario = :idUsuario)
          AND (:soloEnviadas = false OR c.iniciador.idUsuario = :idUsuario)
        ORDER BY COALESCE(c.fechaUltimoMensaje, c.fechaSolicitud) DESC,
                 c.idConversacion DESC
        """,
        countQuery = """
        SELECT COUNT(p)
        FROM ConversacionParticipante p
        JOIN p.conversacion c
        WHERE p.usuario.idUsuario = :idUsuario
          AND p.archivada = :archivada
          AND c.estado IN :estados
          AND (:soloRecibidas = false OR c.destinatario.idUsuario = :idUsuario)
          AND (:soloEnviadas = false OR c.iniciador.idUsuario = :idUsuario)
        """)
    Page<ConversacionParticipante> listarBandeja(
            @Param("idUsuario") Long idUsuario,
            @Param("archivada") boolean archivada,
            @Param("estados") Collection<EstadoConversacion> estados,
            @Param("soloRecibidas") boolean soloRecibidas,
            @Param("soloEnviadas") boolean soloEnviadas,
            Pageable pageable);
}
