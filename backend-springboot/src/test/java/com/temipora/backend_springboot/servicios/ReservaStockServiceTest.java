package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoCompra;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.EstadoReserva;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobroGrupo;
import com.temipora.backend_springboot.modelo.ReservaStock;
import com.temipora.backend_springboot.repositorios.CompraRepository;
import com.temipora.backend_springboot.repositorios.IntentoCobroGrupoRepository;
import com.temipora.backend_springboot.repositorios.IntentoCobroRepository;
import com.temipora.backend_springboot.repositorios.ReservaStockRepository;

@ExtendWith(MockitoExtension.class)
class ReservaStockServiceTest {

    @Mock private ReservaStockRepository reservaRepository;
    @Mock private CompraRepository compraRepository;
    @Mock private IntentoCobroGrupoRepository intentoCobroGrupoRepository;
    @Mock private IntentoCobroRepository intentoCobroRepository;

    @InjectMocks private ReservaStockService service;

    @Test
    void expirarVencidas_liberaReservaYVenceGrupoEIntento() {
        Compra compra = new Compra();
        compra.setEstado(EstadoCompra.PENDIENTE_PAGO);
        CompraGrupo grupo = new CompraGrupo();
        grupo.setEstado(EstadoCompraGrupo.PENDIENTE);
        compra.agregarGrupo(grupo);
        CompraItem item = new CompraItem();
        item.setEstadoEntrega(EstadoEntregaItem.PENDIENTE);
        grupo.agregarItem(item);

        ReservaStock reserva = new ReservaStock();
        reserva.setCompraItem(item);
        reserva.setEstado(EstadoReserva.ACTIVA);
        reserva.setExpiraEn(LocalDateTime.now().minusMinutes(5));

        IntentoCobro intento = new IntentoCobro();
        intento.setEstado(EstadoIntentoCobro.PENDIENTE);
        IntentoCobroGrupo asignacion = new IntentoCobroGrupo();
        asignacion.setGrupo(grupo);
        asignacion.setEstado(EstadoIntentoCobro.PENDIENTE);
        intento.agregarGrupo(asignacion);

        when(reservaRepository.findByEstadoAndExpiraEnBefore(
            eq(EstadoReserva.ACTIVA), any(LocalDateTime.class)))
            .thenReturn(List.of(reserva));
        when(intentoCobroGrupoRepository
            .findFirstByGrupoIdCompraGrupoAndEstadoOrderByIntentoCobroFechaCreacionDesc(
                any(), eq(EstadoIntentoCobro.PENDIENTE)))
            .thenReturn(Optional.of(asignacion));

        service.expirarVencidas();

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.VENCIDA);
        assertThat(item.getEstadoEntrega()).isEqualTo(EstadoEntregaItem.CANCELADO);
        assertThat(grupo.getEstado()).isEqualTo(EstadoCompraGrupo.VENCIDO);
        assertThat(intento.getEstado()).isEqualTo(EstadoIntentoCobro.EXPIRADO);
        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.VENCIDA);
        verify(reservaRepository).save(reserva);
        verify(compraRepository).save(compra);
    }

    @Test
    void expirarVencidas_sinReservas_noHaceNada() {
        when(reservaRepository.findByEstadoAndExpiraEnBefore(
            eq(EstadoReserva.ACTIVA), any(LocalDateTime.class)))
            .thenReturn(List.of());

        service.expirarVencidas();

        verify(reservaRepository, org.mockito.Mockito.never()).save(any());
        verify(compraRepository, org.mockito.Mockito.never()).save(any());
    }
}
