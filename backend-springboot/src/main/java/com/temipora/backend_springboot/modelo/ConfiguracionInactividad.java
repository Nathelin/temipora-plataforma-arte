package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracion_inactividad")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionInactividad {

    public static final Long ID_UNICO = 1L;

    @Id
    @Column(name = "id_configuracion", nullable = false)
    private Long idConfiguracion = ID_UNICO;

    @Column(name = "dias_sin_compra", nullable = false)
    private int diasSinCompra = 60;

    @Column(name = "dias_sin_conexion", nullable = false)
    private int diasSinConexion = 30;

    @Column(name = "dias_cooldown", nullable = false)
    private int diasCooldown = 30;

    @Column(name = "dias_minimos_cuenta", nullable = false)
    private int diasMinimosCuenta = 7;

    @Column(name = "porcentaje_descuento", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeDescuento = new BigDecimal("10.00");

    @Column(name = "vigencia_horas", nullable = false)
    private int vigenciaHoras = 72;

    @Column(name = "max_ofertas_por_usuario", nullable = false)
    private int maxOfertasPorUsuario = 3;

    @Column(name = "max_usuarios_por_corrida", nullable = false)
    private int maxUsuariosPorCorrida = 50;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
