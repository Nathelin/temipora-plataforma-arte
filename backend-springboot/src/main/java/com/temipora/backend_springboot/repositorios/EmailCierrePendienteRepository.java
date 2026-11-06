package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EmailCierrePendiente;
import com.temipora.backend_springboot.modelo.EstadoEmailCierre;
import com.temipora.backend_springboot.modelo.MotivoSustitucion;

public interface EmailCierrePendienteRepository
        extends JpaRepository<EmailCierrePendiente, Long> {

    boolean existsBySolicitudIdSolicitudAndMotivo(
            Long idSolicitud, MotivoSustitucion motivo);

    boolean existsByPedidoIdPedidoAndMotivo(
            Long idPedido, MotivoSustitucion motivo);

    @Query("""
        SELECT DISTINCT e.usuario.idUsuario
          FROM EmailCierrePendiente e
         WHERE e.estado = :estado
           AND e.creadoEn <= :limite
         ORDER BY e.usuario.idUsuario
        """)
    List<Long> usuariosConPendientes(
            @Param("estado") EstadoEmailCierre estado,
            @Param("limite") LocalDateTime limite);

    List<EmailCierrePendiente> findByUsuarioIdUsuarioAndEstadoOrderByCreadoEnAsc(
            Long idUsuario, EstadoEmailCierre estado, Pageable pageable);

    long countByEstado(EstadoEmailCierre estado);
}
