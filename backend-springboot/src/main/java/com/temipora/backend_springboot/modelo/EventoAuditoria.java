package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evento_auditoria")
@Getter
@Setter
@NoArgsConstructor
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_auditoria")
    private Long idEventoAuditoria;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_rol", length = 20)
    private String actorRol;

    @Column(name = "actor_nombre", length = 60)
    private String actorNombre;

    @Column(name = "accion", nullable = false, length = 60)
    private String accion;

    @Column(name = "entidad", length = 60)
    private String entidad;

    @Column(name = "entidad_id", length = 60)
    private String entidadId;

    @Column(name = "resumen", nullable = false, length = 500)
    private String resumen;

    @Column(name = "cambios", nullable = false, columnDefinition = "text")
    private String cambios;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "request_id", length = 36)
    private String requestId;

    @Column(name = "origen", nullable = false, length = 20)
    private String origen;

    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado;
}
