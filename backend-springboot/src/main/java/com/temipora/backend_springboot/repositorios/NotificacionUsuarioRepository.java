package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.NotificacionUsuario;

public interface NotificacionUsuarioRepository extends JpaRepository<NotificacionUsuario, Long> {

    @Query(value = """
        SELECT nu
        FROM NotificacionUsuario nu
        JOIN FETCH nu.notificacion n
        JOIN FETCH n.tipoNotificacion t
        WHERE nu.usuario.idUsuario = :idUsuario
          AND (:soloNoLeidas = false OR nu.leida = false)
          AND (:categoria = '' OR t.categoria = :categoria)
        ORDER BY n.fechaCreacion DESC, n.idNotificacion DESC
        """,
        countQuery = """
        SELECT COUNT(nu)
        FROM NotificacionUsuario nu
        JOIN nu.notificacion n
        JOIN n.tipoNotificacion t
        WHERE nu.usuario.idUsuario = :idUsuario
          AND (:soloNoLeidas = false OR nu.leida = false)
          AND (:categoria = '' OR t.categoria = :categoria)
        """)
    Page<NotificacionUsuario> listarPaginado(
            @Param("idUsuario") Long idUsuario,
            @Param("categoria") String categoria,
            @Param("soloNoLeidas") boolean soloNoLeidas,
            Pageable pageable);

    @Query("""
        SELECT nu
        FROM NotificacionUsuario nu
        JOIN FETCH nu.notificacion n
        JOIN FETCH n.tipoNotificacion
        WHERE nu.usuario.idUsuario = :idUsuario
        ORDER BY nu.leida ASC, n.fechaCreacion DESC, n.idNotificacion DESC
        """)
    List<NotificacionUsuario> listarRecientes(
            @Param("idUsuario") Long idUsuario,
            Pageable pageable);

    @Query("""
        SELECT t.categoria, COUNT(nu)
        FROM NotificacionUsuario nu
        JOIN nu.notificacion n
        JOIN n.tipoNotificacion t
        WHERE nu.usuario.idUsuario = :idUsuario AND nu.leida = false
        GROUP BY t.categoria
        """)
    List<Object[]> contarNoLeidasPorCategoria(@Param("idUsuario") Long idUsuario);

    long countByUsuarioIdUsuarioAndLeidaFalse(Long idUsuario);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE NotificacionUsuario nu
        SET nu.leida = true, nu.fechaLectura = :fecha
        WHERE nu.usuario.idUsuario = :idUsuario AND nu.leida = false
        """)
    int marcarTodasLeidas(
            @Param("idUsuario") Long idUsuario,
            @Param("fecha") LocalDateTime fecha);

    Optional<NotificacionUsuario> findByIdNotificacionUsuarioAndUsuarioIdUsuario(
            Long idNotificacionUsuario, Long idUsuario);

    Optional<NotificacionUsuario> findByNotificacionIdNotificacionAndUsuarioIdUsuario(
            Long idNotificacion, Long idUsuario);
}
