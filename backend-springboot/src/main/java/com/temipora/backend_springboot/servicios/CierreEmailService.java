package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionSustitucion;
import com.temipora.backend_springboot.modelo.EmailCierrePendiente;
import com.temipora.backend_springboot.modelo.EstadoEmailCierre;
import com.temipora.backend_springboot.modelo.MotivoSustitucion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EmailCierrePendienteRepository;

/**
 * Cola de mails de cierre. El alta es barata (solo una fila); el envío lo
 * hace el job agrupando por usuario.
 */
@Service
public class CierreEmailService {

    private final EmailCierrePendienteRepository repository;
    private final ConfiguracionSustitucionService configuracionService;
    private final CierreEmailProcesador procesador;

    public CierreEmailService(
            EmailCierrePendienteRepository repository,
            ConfiguracionSustitucionService configuracionService,
            CierreEmailProcesador procesador) {
        this.repository = repository;
        this.configuracionService = configuracionService;
        this.procesador = procesador;
    }

    @Transactional
    public void encolar(
            Usuario cliente,
            Solicitud solicitud,
            Pedido pedido,
            MotivoSustitucion motivo,
            LocalDateTime fechaEvento) {
        if (cliente == null || cliente.getIdUsuario() == null
                || cliente.getCorreo() == null
                || cliente.getCorreo().isBlank()) {
            return;
        }
        ConfiguracionSustitucion config = configuracionService.obtener();
        if (!config.isEmailCierreHabilitado()) {
            return;
        }
        Long idSolicitud = solicitud != null ? solicitud.getIdSolicitud() : null;
        Long idPedido = pedido != null ? pedido.getIdPedido() : null;
        if (idSolicitud == null && idPedido == null) {
            return;
        }
        if (idSolicitud != null
                && repository.existsBySolicitudIdSolicitudAndMotivo(
                    idSolicitud, motivo)) {
            return;
        }
        if (idPedido != null
                && repository.existsByPedidoIdPedidoAndMotivo(idPedido, motivo)) {
            return;
        }

        EmailCierrePendiente fila = new EmailCierrePendiente();
        fila.setUsuario(cliente);
        fila.setSolicitud(solicitud);
        fila.setPedido(pedido);
        fila.setMotivo(motivo);
        fila.setFechaEvento(fechaEvento != null
            ? fechaEvento
            : LocalDateTime.now());
        fila.setEstado(EstadoEmailCierre.PENDIENTE);
        repository.save(fila);
    }

    /**
     * Toma los usuarios con cierres pendientes que ya cumplieron la ventana
     * y delega el envío en un procesador transaccional por usuario.
     */
    public int procesarPendientes() {
        return procesarPendientes(false);
    }

    /**
     * @param forzar si es true ignora la ventana de agrupación. Lo usa el
     *               botón del panel para probar sin esperar al cron.
     */
    public int procesarPendientes(boolean forzar) {
        ConfiguracionSustitucion config = configuracionService.obtener();
        if (!config.isEmailCierreHabilitado()) {
            return 0;
        }
        LocalDateTime limite = forzar
            ? LocalDateTime.now()
            : LocalDateTime.now().minusMinutes(
                Math.max(0, config.getEmailCierreMinutos()));
        List<Long> usuarios = repository.usuariosConPendientes(
            EstadoEmailCierre.PENDIENTE, limite);
        int enviados = 0;
        for (Long idUsuario : usuarios) {
            try {
                if (procesador.procesar(idUsuario)) {
                    enviados++;
                }
            } catch (RuntimeException e) {
                // El procesador ya registró el intento; seguimos con el resto.
            }
        }
        return enviados;
    }

    @Transactional(readOnly = true)
    public long pendientes() {
        return repository.countByEstado(EstadoEmailCierre.PENDIENTE);
    }
}
