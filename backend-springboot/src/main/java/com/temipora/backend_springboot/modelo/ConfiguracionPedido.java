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
@Table(name = "configuracion_pedido")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionPedido {

    public static final Long ID_UNICO = 1L;
    public static final int DIAS_APROBACION_POR_DEFECTO = 7;
    public static final int DIAS_PAGO_POR_DEFECTO = 7;
    public static final int REVISIONES_MAXIMAS_POR_DEFECTO = 2;
    public static final int DIAS_REENTREGA_POR_DEFECTO = 7;
    public static final int MAX_HITOS_POR_DEFECTO = 4;
    public static final BigDecimal PORCENTAJE_MAX_PRIMER_HITO_POR_DEFECTO =
        new BigDecimal("50.00");
    public static final BigDecimal MONTO_MINIMO_CUOTA_POR_DEFECTO =
        new BigDecimal("1.00");

    @Id
    @Column(name = "id_configuracion", nullable = false)
    private Long idConfiguracion = ID_UNICO;

    @Column(name = "dias_aprobacion_automatica", nullable = false)
    private Integer diasAprobacionAutomatica = DIAS_APROBACION_POR_DEFECTO;

    @Column(name = "dias_pago_cuota", nullable = false)
    private Integer diasPagoCuota = DIAS_PAGO_POR_DEFECTO;

    @Column(name = "revisiones_maximas_por_hito", nullable = false)
    private Integer revisionesMaximasPorHito = REVISIONES_MAXIMAS_POR_DEFECTO;

    @Column(name = "dias_reentrega", nullable = false)
    private Integer diasReentrega = DIAS_REENTREGA_POR_DEFECTO;

    @Column(name = "max_hitos", nullable = false)
    private Integer maxHitos = MAX_HITOS_POR_DEFECTO;

    @Column(name = "porcentaje_max_primer_hito", nullable = false,
        precision = 5, scale = 2)
    private BigDecimal porcentajeMaxPrimerHito = PORCENTAJE_MAX_PRIMER_HITO_POR_DEFECTO;

    @Column(name = "monto_minimo_cuota", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoMinimoCuota = MONTO_MINIMO_CUOTA_POR_DEFECTO;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
