package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracion_mensajeria")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionMensajeria {

    public static final Long ID_UNICO = 1L;
    public static final int LONGITUD_MAXIMA_MENSAJE_POR_DEFECTO = 2000;
    public static final int LONGITUD_MAXIMA_MOTIVO_POR_DEFECTO = 200;
    public static final int HORAS_ESPERA_REENVIO_POR_DEFECTO = 168;
    public static final int MAX_SOLICITUDES_PENDIENTES_POR_DEFECTO = 5;
    public static final int MAX_SOLICITUDES_POR_DIA_POR_DEFECTO = 10;
    public static final int MAX_MENSAJES_POR_DIA_POR_DEFECTO = 100;
    public static final int DIAS_VENCIMIENTO_SOLICITUD_POR_DEFECTO = 30;

    @Id
    @Column(name = "id_configuracion", nullable = false)
    private Long idConfiguracion = ID_UNICO;

    @Column(name = "longitud_maxima_mensaje", nullable = false)
    private Integer longitudMaximaMensaje = LONGITUD_MAXIMA_MENSAJE_POR_DEFECTO;

    @Column(name = "longitud_maxima_motivo", nullable = false)
    private Integer longitudMaximaMotivo = LONGITUD_MAXIMA_MOTIVO_POR_DEFECTO;

    @Column(name = "horas_espera_reenvio", nullable = false)
    private Integer horasEsperaReenvio = HORAS_ESPERA_REENVIO_POR_DEFECTO;

    @Column(name = "max_solicitudes_pendientes", nullable = false)
    private Integer maxSolicitudesPendientes = MAX_SOLICITUDES_PENDIENTES_POR_DEFECTO;

    @Column(name = "max_solicitudes_por_dia", nullable = false)
    private Integer maxSolicitudesPorDia = MAX_SOLICITUDES_POR_DIA_POR_DEFECTO;

    @Column(name = "max_mensajes_por_dia", nullable = false)
    private Integer maxMensajesPorDia = MAX_MENSAJES_POR_DIA_POR_DEFECTO;

    @Column(name = "dias_vencimiento_solicitud", nullable = false)
    private Integer diasVencimientoSolicitud = DIAS_VENCIMIENTO_SOLICITUD_POR_DEFECTO;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
