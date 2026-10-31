package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ConversacionAccion;
import com.temipora.backend_springboot.modelo.TipoAccionConversacion;

public interface ConversacionAccionRepository
        extends JpaRepository<ConversacionAccion, Long> {

    long countByUsuarioIdUsuarioAndTipoAccionInAndFechaAfter(
            Long idUsuario,
            Collection<TipoAccionConversacion> tipos,
            LocalDateTime desde);

    Optional<ConversacionAccion>
        findFirstByConversacionIdConversacionAndTipoAccionOrderByFechaDesc(
            Long idConversacion, TipoAccionConversacion tipoAccion);
}
