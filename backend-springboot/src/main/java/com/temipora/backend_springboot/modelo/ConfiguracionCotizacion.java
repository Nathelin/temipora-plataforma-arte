package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracion_cotizacion")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionCotizacion {

    public static final Long ID_UNICO = 1L;

    @Id
    @Column(name = "id_configuracion", nullable = false)
    private Long idConfiguracion = ID_UNICO;

    @Column(name = "api_habilitada", nullable = false)
    private boolean apiHabilitada = true;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda_origen", nullable = false)
    private Moneda monedaOrigen;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda_destino", nullable = false)
    private Moneda monedaDestino;

    @Column(name = "factor_manual", precision = 18, scale = 8)
    private BigDecimal factorManual;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
