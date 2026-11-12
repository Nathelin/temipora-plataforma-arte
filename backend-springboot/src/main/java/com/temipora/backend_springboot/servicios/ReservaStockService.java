package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoCompra;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.EstadoPublicacionReventa;
import com.temipora.backend_springboot.modelo.EstadoReserva;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobroGrupo;
import com.temipora.backend_springboot.modelo.ReservaStock;
import com.temipora.backend_springboot.repositorios.CompraRepository;
import com.temipora.backend_springboot.repositorios.IntentoCobroGrupoRepository;
import com.temipora.backend_springboot.repositorios.IntentoCobroRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
import com.temipora.backend_springboot.repositorios.ReservaStockRepository;

@Service
public class ReservaStockService {

    private final ReservaStockRepository reservaRepository;
    private final CompraRepository compraRepository;
    private final IntentoCobroGrupoRepository intentoCobroGrupoRepository;
    private final IntentoCobroRepository intentoCobroRepository;
    private final PublicacionReventaRepository publicacionReventaRepository;

    public ReservaStockService(
            ReservaStockRepository reservaRepository,
            CompraRepository compraRepository,
            IntentoCobroGrupoRepository intentoCobroGrupoRepository,
            IntentoCobroRepository intentoCobroRepository,
            PublicacionReventaRepository publicacionReventaRepository) {
        this.reservaRepository = reservaRepository;
        this.compraRepository = compraRepository;
        this.intentoCobroGrupoRepository = intentoCobroGrupoRepository;
        this.intentoCobroRepository = intentoCobroRepository;
        this.publicacionReventaRepository = publicacionReventaRepository;
    }

    @Transactional
    public void expirarVencidas() {
        LocalDateTime ahora = LocalDateTime.now();
        List<ReservaStock> vencidas = reservaRepository
            .findByEstadoAndExpiraEnBefore(EstadoReserva.ACTIVA, ahora);
        Set<Long> gruposProcesados = new HashSet<>();

        for (ReservaStock reserva : vencidas) {
            reserva.setEstado(EstadoReserva.VENCIDA);
            reserva.setFechaActualizacion(ahora);
            reservaRepository.save(reserva);

            CompraItem item = reserva.getCompraItem();
            if (item == null) {
                continue;
            }
            item.setEstadoEntrega(EstadoEntregaItem.CANCELADO);
            liberarPublicacion(item, ahora);
            CompraGrupo grupo = item.getGrupo();
            if (grupo == null) {
                continue;
            }
            if (!gruposProcesados.add(grupo.getIdCompraGrupo())) {
                continue;
            }
            if (grupo.getEstado() == EstadoCompraGrupo.PENDIENTE) {
                grupo.setEstado(EstadoCompraGrupo.VENCIDO);
                grupo.setFechaActualizacion(ahora);
            }
            expirarIntentos(grupo, ahora);

            Compra compra = grupo.getCompra();
            actualizarEstadoCompra(compra, ahora);
            compraRepository.save(compra);
        }
    }

    private void liberarPublicacion(CompraItem item, LocalDateTime ahora) {
        if (item.getIdPublicacionReventa() == null) {
            return;
        }
        publicacionReventaRepository
            .findByIdPublicacionReventaForUpdate(item.getIdPublicacionReventa())
            .filter(publicacion ->
                publicacion.getEstado() == EstadoPublicacionReventa.RESERVADA)
            .ifPresent(publicacion -> {
                publicacion.setEstado(EstadoPublicacionReventa.PUBLICADA);
                publicacion.setFechaActualizacion(ahora);
                publicacionReventaRepository.save(publicacion);
            });
    }

    private void expirarIntentos(CompraGrupo grupo, LocalDateTime ahora) {
        IntentoCobroGrupo asignacion = intentoCobroGrupoRepository
            .findFirstByGrupoIdCompraGrupoAndEstadoOrderByIntentoCobroFechaCreacionDesc(
                grupo.getIdCompraGrupo(), EstadoIntentoCobro.PENDIENTE)
            .orElse(null);
        if (asignacion == null) {
            return;
        }
        asignacion.setEstado(EstadoIntentoCobro.EXPIRADO);
        IntentoCobro intento = asignacion.getIntentoCobro();
        intento.setEstado(EstadoIntentoCobro.EXPIRADO);
        intento.setFechaActualizacion(ahora);
        intentoCobroRepository.save(intento);
    }

    private void actualizarEstadoCompra(Compra compra, LocalDateTime ahora) {
        boolean hayPendientes = compra.getGrupos().stream()
            .anyMatch(grupo -> grupo.getEstado() == EstadoCompraGrupo.PENDIENTE);
        boolean hayPagados = compra.getGrupos().stream()
            .anyMatch(grupo -> grupo.getEstado() == EstadoCompraGrupo.PAGADO);

        if (hayPendientes) {
            compra.setEstado(hayPagados
                ? EstadoCompra.PARCIALMENTE_PAGA
                : EstadoCompra.PENDIENTE_PAGO);
        } else if (compra.getGrupos().stream()
                .allMatch(grupo -> grupo.getEstado() == EstadoCompraGrupo.PAGADO)) {
            compra.setEstado(EstadoCompra.PAGA);
        } else if (hayPagados) {
            compra.setEstado(EstadoCompra.PARCIALMENTE_PAGA);
        } else {
            compra.setEstado(EstadoCompra.VENCIDA);
        }
        compra.setFechaActualizacion(ahora);
    }
}
