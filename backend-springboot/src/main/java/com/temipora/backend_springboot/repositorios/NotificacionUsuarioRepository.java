package com.temipora.backend_springboot.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.NotificacionUsuario;

public interface NotificacionUsuarioRepository extends JpaRepository<NotificacionUsuario, Long> {

    @Query("""
        SELECT nu
        FROM NotificacionUsuario nu
        JOIN FETCH nu.notificacion n
        JOIN FETCH n.tipoNotificacion
        WHERE nu.usuario.idUsuario = :idUsuario
        ORDER BY n.fechaCreacion DESC
        """)
    List<NotificacionUsuario> listarPorUsuario(@Param("idUsuario") Long idUsuario);

    long countByUsuarioIdUsuarioAndLeidaFalse(Long idUsuario);

    Optional<NotificacionUsuario> findByIdNotificacionUsuarioAndUsuarioIdUsuario(
            Long idNotificacionUsuario, Long idUsuario);

    Optional<NotificacionUsuario> findByNotificacionIdNotificacionAndUsuarioIdUsuario(
            Long idNotificacion, Long idUsuario);

    @Query("""
        SELECT COUNT(nu)
        FROM NotificacionUsuario nu
        JOIN nu.notificacion n
        JOIN n.tipoNotificacion t
        WHERE nu.usuario.idUsuario = :idUsuario
          AND nu.leida = false
          AND t.codigo = :codigo
          AND n.idReferencia = :idReferencia
          AND n.tipoReferencia = :tipoReferencia
        """)
    long contarNoLeidasPorReferencia(
            @Param("idUsuario") Long idUsuario,
            @Param("codigo") String codigo,
            @Param("idReferencia") Long idReferencia,
            @Param("tipoReferencia") String tipoReferencia);
}
