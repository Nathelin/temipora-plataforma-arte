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
@Table(name = "configuracion_baja_cuenta")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionBajaCuenta {

    public static final Long ID_UNICO = 1L;
    public static final int DIAS_POR_DEFECTO = 30;

    @Id
    @Column(name = "id_configuracion", nullable = false)
    private Long idConfiguracion = ID_UNICO;

    @Column(name = "dias_para_reactivar", nullable = false)
    private Integer diasParaReactivar = DIAS_POR_DEFECTO;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
