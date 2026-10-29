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
@Table(name = "configuracion_resenia")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionResenia {

    public static final Long ID_UNICO = 1L;
    public static final int DIAS_VENTANA_CREACION_POR_DEFECTO = 30;
    public static final int DIAS_EDICION_POR_DEFECTO = 3;
    public static final int MINIMO_RESENIAS_ETIQUETA_POR_DEFECTO = 3;
    public static final BigDecimal UMBRAL_EXCELENTE_POR_DEFECTO = new BigDecimal("4.50");
    public static final BigDecimal UMBRAL_BUENA_POR_DEFECTO = new BigDecimal("3.50");

    @Id
    @Column(name = "id_configuracion", nullable = false)
    private Long idConfiguracion = ID_UNICO;

    @Column(name = "dias_ventana_creacion", nullable = false)
    private Integer diasVentanaCreacion = DIAS_VENTANA_CREACION_POR_DEFECTO;

    @Column(name = "dias_edicion", nullable = false)
    private Integer diasEdicion = DIAS_EDICION_POR_DEFECTO;

    @Column(name = "minimo_resenias_etiqueta", nullable = false)
    private Integer minimoReseniasEtiqueta = MINIMO_RESENIAS_ETIQUETA_POR_DEFECTO;

    @Column(name = "umbral_excelente", nullable = false, precision = 3, scale = 2)
    private BigDecimal umbralExcelente = UMBRAL_EXCELENTE_POR_DEFECTO;

    @Column(name = "umbral_buena", nullable = false, precision = 3, scale = 2)
    private BigDecimal umbralBuena = UMBRAL_BUENA_POR_DEFECTO;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
