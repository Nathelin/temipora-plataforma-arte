package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversacion")
@Getter
@Setter
@NoArgsConstructor
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idConversacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_iniciador", nullable = false)
    private Usuario iniciador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_destinatario", nullable = false)
    private Usuario destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoConversacion estado = EstadoConversacion.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrigenConversacion origen = OrigenConversacion.SOLICITUD_MENSAJE;

    @Column(name = "motivo_contacto", nullable = false, length = 200)
    private String motivoContacto;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @Column(name = "fecha_ultimo_mensaje")
    private LocalDateTime fechaUltimoMensaje;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public boolean involucra(Long idUsuario) {
        return idUsuario != null
            && (idUsuario.equals(iniciador.getIdUsuario())
                || idUsuario.equals(destinatario.getIdUsuario()));
    }

    public boolean esIniciador(Long idUsuario) {
        return idUsuario != null && idUsuario.equals(iniciador.getIdUsuario());
    }

    public boolean esDestinatario(Long idUsuario) {
        return idUsuario != null && idUsuario.equals(destinatario.getIdUsuario());
    }

    public Usuario contraparteDe(Long idUsuario) {
        return esIniciador(idUsuario) ? destinatario : iniciador;
    }
}
