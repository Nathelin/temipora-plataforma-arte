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
@Table(name = "configuracion_sustitucion")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionSustitucion {

    public static final Long ID_UNICO = 1L;

    @Id
    @Column(name = "id_configuracion", nullable = false)
    private Long idConfiguracion = ID_UNICO;

    @Column(nullable = false)
    private boolean habilitado = true;

    @Column(name = "max_alternativas", nullable = false)
    private int maxAlternativas = 6;

    @Column(name = "max_por_artista", nullable = false)
    private int maxPorArtista = 1;

    @Column(name = "rango_precio_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal rangoPrecioPct = new BigDecimal("40.00");

    @Column(name = "bonus_favorito", nullable = false, precision = 5, scale = 3)
    private BigDecimal bonusFavorito = new BigDecimal("0.100");

    @Column(name = "factor_tiempo_max", nullable = false, precision = 4, scale = 2)
    private BigDecimal factorTiempoMax = new BigDecimal("3.00");

    @Column(name = "mostrar_en_explorar", nullable = false)
    private boolean mostrarEnExplorar = true;

    @Column(name = "minutos_dedupe", nullable = false)
    private int minutosDedupe = 30;

    @Column(name = "dias_retencion", nullable = false)
    private int diasRetencion = 180;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
