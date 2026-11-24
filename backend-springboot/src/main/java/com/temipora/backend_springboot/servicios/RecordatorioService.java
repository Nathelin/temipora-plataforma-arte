package com.temipora.backend_springboot.servicios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EmailRecordatorioPendiente;
import com.temipora.backend_springboot.modelo.EstadoEmailRecordatorio;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.MotivoRecordatorio;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.repositorios.EmailRecordatorioPendienteRepository;
import com.temipora.backend_springboot.repositorios.HitoPedidoRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;

/**
 * Encola los recordatorios de pago (clientes) y de entrega (artistas) y
 * dispara el envío agrupado por usuario.
 */
@Service
public class RecordatorioService {

    private static final int HORAS_ESPERA_SENA = 24;
    private static final int HORAS_AVISO_72 = 72;
    private static final int HORAS_AVISO_24 = 24;
    private static final int DIAS_AVISO_SEMANAL = 7;

    private final HitoPedidoRepository hitoPedidoRepository;
    private final PedidoRepository pedidoRepository;
    private final EmailRecordatorioPendienteRepository recordatorioRepository;
    private final SancionService sancionService;
    private final RecordatorioProcesador procesador;
    private final EntregaPendienteService entregaPendienteService;

    public RecordatorioService(
            HitoPedidoRepository hitoPedidoRepository,
            PedidoRepository pedidoRepository,
            EmailRecordatorioPendienteRepository recordatorioRepository,
            SancionService sancionService,
            RecordatorioProcesador procesador,
            EntregaPendienteService entregaPendienteService) {
        this.hitoPedidoRepository = hitoPedidoRepository;
        this.pedidoRepository = pedidoRepository;
        this.recordatorioRepository = recordatorioRepository;
        this.sancionService = sancionService;
        this.procesador = procesador;
        this.entregaPendienteService = entregaPendienteService;
    }

    @Transactional
    public int encolar() {
        LocalDateTime ahora = LocalDateTime.now();
        return encolarPagos(ahora) + encolarEntregas(LocalDate.now());
    }

    public int procesarPendientes() {
        List<Long> usuarios = recordatorioRepository.findUsuariosConPendientes(
            EstadoEmailRecordatorio.PENDIENTE);
        int enviados = 0;
        for (Long idUsuario : usuarios) {
            if (procesador.procesar(idUsuario)) {
                enviados++;
            }
        }
        return enviados;
    }

    /**
     * Reabre los recordatorios ya enviados (o fallidos) para poder reenviarlos.
     * Pensado para pruebas y demos: el envío normal es idempotente y no repite
     * un recordatorio ya enviado. El procesador vuelve a validar las
     * condiciones y omite los que ya no corresponden.
     */
    @Transactional
    public int reabrirEnviados() {
        List<EmailRecordatorioPendiente> filas = recordatorioRepository.findByEstadoIn(
            List.of(EstadoEmailRecordatorio.ENVIADO,
                EstadoEmailRecordatorio.FALLIDO));
        for (EmailRecordatorioPendiente fila : filas) {
            fila.setEstado(EstadoEmailRecordatorio.PENDIENTE);
            fila.setIntentos(0);
            fila.setEnviadoEn(null);
            fila.setIdLote(null);
        }
        recordatorioRepository.saveAll(filas);
        return filas.size();
    }

    private int encolarPagos(LocalDateTime ahora) {
        int total = 0;

        List<HitoPedido> senas = hitoPedidoRepository.findSenasPendientes(
            EstadoPagoHito.PENDIENTE,
            TipoHito.SENA,
            EstadoPedido.PENDIENTE_PAGO,
            ahora.minusHours(HORAS_ESPERA_SENA));
        for (HitoPedido hito : senas) {
            total += encolarPago(hito, MotivoRecordatorio.PAGO_SENA);
        }

        // Aviso semanal solo para la entrega final, que se paga después de la
        // entrega y tiene una ventana más larga.
        List<HitoPedido> entregasPorVencer =
            hitoPedidoRepository.findPagosPorVencer(
                EstadoPagoHito.PENDIENTE,
                List.of(TipoHito.ENTREGA),
                List.of(EstadoPedido.EN_CURSO),
                ahora,
                ahora.plusDays(DIAS_AVISO_SEMANAL));
        for (HitoPedido hito : entregasPorVencer) {
            LocalDateTime limite = hito.getFechaLimitePago();
            if (limite == null
                    || !limite.isAfter(ahora.plusHours(HORAS_AVISO_72))) {
                continue;
            }
            total += encolarPago(hito, MotivoRecordatorio.PAGO_7D);
        }

        List<HitoPedido> porVencer = hitoPedidoRepository.findPagosPorVencer(
            EstadoPagoHito.PENDIENTE,
            List.of(TipoHito.SENA, TipoHito.CUOTA, TipoHito.ENTREGA),
            List.of(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.EN_CURSO),
            ahora,
            ahora.plusHours(HORAS_AVISO_72));
        for (HitoPedido hito : porVencer) {
            LocalDateTime limite = hito.getFechaLimitePago();
            if (limite == null) {
                continue;
            }
            boolean dentroDe24 = !limite.isAfter(
                ahora.plusHours(HORAS_AVISO_24));
            if (hito.getHitoPresupuesto().getTipo() == TipoHito.SENA
                    && !dentroDe24) {
                // La seña ya tiene su aviso del día siguiente.
                continue;
            }
            total += encolarPago(hito,
                dentroDe24
                    ? MotivoRecordatorio.PAGO_24H
                    : MotivoRecordatorio.PAGO_72H);
        }
        return total;
    }

    private int encolarPago(HitoPedido hito, MotivoRecordatorio motivo) {
        if (hito.getFechaLimitePago() == null
                || !hito.getFechaLimitePago().isAfter(LocalDateTime.now())) {
            return 0;
        }
        Pedido pedido = hito.getPedido();
        if (pedido == null || pedido.getCliente() == null) {
            return 0;
        }
        // Los recordatorios de pago no se suprimen por una sanción: son el
        // aviso que le permite al cliente desbloquear la cuenta.
        String clave = clave(motivo, pedido, hito, hito.getFechaLimitePago());
        if (recordatorioRepository.existsByClave(clave)) {
            return 0;
        }
        EmailRecordatorioPendiente fila = new EmailRecordatorioPendiente();
        fila.setUsuario(pedido.getCliente());
        fila.setPedido(pedido);
        fila.setHitoPedido(hito);
        fila.setMotivo(motivo);
        fila.setClave(clave);
        fila.setFechaReferencia(hito.getFechaLimitePago());
        recordatorioRepository.save(fila);
        return 1;
    }

    private int encolarEntregas(LocalDate hoy) {
        int total = 0;
        List<Pedido> pedidos = pedidoRepository.findConHitosByEstado(
            EstadoPedido.EN_CURSO);
        for (Pedido pedido : pedidos) {
            LocalDate prevista = PedidoFechas.entregaPrevista(pedido);
            if (prevista == null) {
                continue;
            }
            long dias = ChronoUnit.DAYS.between(hoy, prevista);
            if (dias < 0 || dias > DIAS_AVISO_SEMANAL) {
                continue;
            }
            if (!entregaPendienteService.artistaDebeEntregar(pedido)) {
                continue;
            }
            long duracion = pedido.getFechaInicio() != null
                ? ChronoUnit.DAYS.between(pedido.getFechaInicio(), prevista)
                : 0;

            MotivoRecordatorio motivo = null;
            if (dias == DIAS_AVISO_SEMANAL && duracion > DIAS_AVISO_SEMANAL) {
                motivo = MotivoRecordatorio.ENTREGA_7D;
            } else if (dias == 1) {
                motivo = MotivoRecordatorio.ENTREGA_1D;
            } else if (dias == 0) {
                motivo = MotivoRecordatorio.ENTREGA_HOY;
            }
            if (motivo == null) {
                continue;
            }
            if (pedido.getArtista() == null
                    || estaSancionado(pedido.getArtista().getIdUsuario())) {
                continue;
            }
            String clave = clave(
                motivo, pedido, null, prevista.atStartOfDay());
            if (recordatorioRepository.existsByClave(clave)) {
                continue;
            }
            EmailRecordatorioPendiente fila = new EmailRecordatorioPendiente();
            fila.setUsuario(pedido.getArtista());
            fila.setPedido(pedido);
            fila.setMotivo(motivo);
            fila.setClave(clave);
            fila.setFechaReferencia(prevista.atStartOfDay());
            recordatorioRepository.save(fila);
            total++;
        }
        return total;
    }

    private boolean estaSancionado(Long idUsuario) {
        return sancionService.obtenerVigente(idUsuario).isPresent();
    }

    private String clave(
            MotivoRecordatorio motivo,
            Pedido pedido,
            HitoPedido hito,
            LocalDateTime referencia) {
        long idHito = hito != null && hito.getIdHitoPedido() != null
            ? hito.getIdHitoPedido()
            : 0L;
        return motivo.name() + ":" + pedido.getIdPedido() + ":" + idHito + ":"
            + (referencia != null ? referencia : "");
    }
}
