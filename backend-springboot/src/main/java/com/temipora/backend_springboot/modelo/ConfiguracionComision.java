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
@Table(name = "configuracion_comision")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionComision {

    public static final Long ID_UNICO = 1L;
    public static final BigDecimal PORCENTAJE_POR_DEFECTO = new BigDecimal("10.00");

    @Id
    @Column(name = "id_configuracion", nullable = false)
    private Long idConfiguracion = ID_UNICO;

    @Column(name = "porcentaje_servicio", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeServicio = PORCENTAJE_POR_DEFECTO;

    @Column(name = "porcentaje_producto", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeProducto = PORCENTAJE_POR_DEFECTO;

    @Column(name = "porcentaje_reventa", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeReventa = PORCENTAJE_POR_DEFECTO;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
