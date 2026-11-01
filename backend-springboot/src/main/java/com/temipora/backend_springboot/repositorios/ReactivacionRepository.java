package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.Usuario;

/**
 * Consulta de candidatos para la campaña de reactivación.
 *
 * Un usuario es candidato si no compró en los últimos N días o si no se
 * logueó en los últimos M días (cualquiera de las dos condiciones alcanza),
 * tiene la cuenta activa, no está en medio de una compra y no recibió una
 * campaña dentro del cooldown.
 */
public interface ReactivacionRepository extends JpaRepository<Usuario, Long> {

    @Query(value = """
        SELECT u.id_usuario
        FROM usuario u
        JOIN rol r ON r.id_rol = u.id_rol
        WHERE u.estado_cuenta = TRUE
          AND r.nombre IN ('CLIENTE', 'ARTISTA')
          AND COALESCE(u.fecha_creacion, CURRENT_TIMESTAMP) <= :fechaLimiteCuenta
          AND (
              COALESCE(
                  (SELECT MAX(p.fecha_creacion) FROM pedido p
                   WHERE p.id_cliente = u.id_usuario),
                  u.fecha_creacion,
                  CURRENT_TIMESTAMP
              ) <= :fechaLimiteCompra
              OR COALESCE(
                  u.ultima_conexion,
                  u.fecha_creacion,
                  CURRENT_TIMESTAMP
              ) <= :fechaLimiteConexion
          )
          AND NOT EXISTS (
              SELECT 1 FROM solicitud s
              WHERE s.id_cliente = u.id_usuario
                AND s.estado IN ('PENDIENTE', 'EN_LISTA_ESPERA',
                                 'PRESUPUESTADA', 'PAGADA')
          )
          AND NOT EXISTS (
              SELECT 1 FROM pedido p
              WHERE p.id_cliente = u.id_usuario
                AND p.estado IN ('PENDIENTE_PAGO', 'EN_CURSO')
          )
          AND NOT EXISTS (
              SELECT 1 FROM campana_reactivacion c
              WHERE c.id_usuario = u.id_usuario
                AND c.fecha_creacion >= :fechaCooldown
          )
        ORDER BY COALESCE(u.ultima_conexion, u.fecha_creacion, CURRENT_TIMESTAMP) ASC
        LIMIT :limite
        """, nativeQuery = true)
    List<Long> candidatos(
        @Param("fechaLimiteCuenta") LocalDateTime fechaLimiteCuenta,
        @Param("fechaLimiteCompra") LocalDateTime fechaLimiteCompra,
        @Param("fechaLimiteConexion") LocalDateTime fechaLimiteConexion,
        @Param("fechaCooldown") LocalDateTime fechaCooldown,
        @Param("limite") int limite);
}
