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
@Table(name = "configuracion_presupuesto")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionPresupuesto {

    public static final Long ID_UNICO = 1L;
    public static final int VIGENCIA_POR_DEFECTO_HORAS = 72;

    @Id
    @Column(name = "id_configuracion", nullable = false)
    private Long idConfiguracion = ID_UNICO;

    @Column(name = "vigencia_horas", nullable = false)
    private Integer vigenciaHoras = VIGENCIA_POR_DEFECTO_HORAS;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
