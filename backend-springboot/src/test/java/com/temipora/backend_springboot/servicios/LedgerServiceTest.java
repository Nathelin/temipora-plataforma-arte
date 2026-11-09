package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EventoLedger;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoLedgerDTO;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoEventoLedger;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.VarianteLedgerDTO;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    private static final String SHA = "b".repeat(64);

    @Mock
    private EventoLedgerRepository eventoRepository;

    @InjectMocks
    private LedgerService ledgerService;

    @Test
    void registrarGenesis_creaEventoEncadenadoConHashYMetadata() {
        ProductoVariante variante = variante(1);
        when(eventoRepository.findTopByVarianteIdVarianteOrderBySecuenciaDesc(20L))
            .thenReturn(Optional.empty());
        when(eventoRepository.save(any(EventoLedger.class)))
            .thenAnswer(inv -> {
                EventoLedger evento = inv.getArgument(0);
                evento.setIdEventoLedger(1L);
                return evento;
            });

        EventoLedger evento = ledgerService.registrarGenesis(variante, "hashTerminos");

        assertThat(evento.getIdEventoLedger()).isEqualTo(1L);
        assertThat(evento.getSecuencia()).isEqualTo(1L);
        assertThat(evento.getTipo()).isEqualTo(TipoEventoLedger.GENESIS);
        assertThat(evento.getHashPrevio()).isEmpty();
        assertThat(evento.getHashImagen()).isEqualTo(SHA);
        assertThat(evento.getHashEvento()).hasSize(64);
        assertThat(evento.getDatos()).contains("Plancha de otoño");
        assertThat(evento.getDatos()).contains("artista");
        assertThat(evento.getActorOrigen().getIdUsuario()).isEqualTo(5L);
        assertThat(evento.getActorDestino().getIdUsuario()).isEqualTo(5L);
        assertThat(evento.getMoneda().getCodigo()).isEqualTo("ARS");
        verify(eventoRepository).save(evento);
    }

    @Test
    void registrarGenesis_esIdempotenteSiYaExiste() {
        ProductoVariante variante = variante(1);
        EventoLedger existente = new EventoLedger();
        existente.setSecuencia(1L);
        existente.setTipo(TipoEventoLedger.GENESIS);
        when(eventoRepository.findTopByVarianteIdVarianteOrderBySecuenciaDesc(20L))
            .thenReturn(Optional.of(existente));

        EventoLedger evento = ledgerService.registrarGenesis(variante, "hashTerminos");

        assertThat(evento).isSameAs(existente);
        verify(eventoRepository, never()).save(any(EventoLedger.class));
    }

    @Test
    void registrarGenesis_varianteNoUnica_lanzaError() {
        ProductoVariante variante = variante(2);

        assertThatThrownBy(() -> ledgerService.registrarGenesis(variante, "hash"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Solo las variantes con stock 1 tienen registro de autenticidad.");
    }

    @Test
    void verificarVariante_sinEventos_noTieneLedgerYEsIntegra() {
        ProductoVariante variante = variante(1);
        when(eventoRepository.findByVarianteIdVarianteOrderBySecuenciaAsc(20L))
            .thenReturn(List.of());

        VarianteLedgerDTO dto = ledgerService.verificarVariante(variante);

        assertThat(dto.isTieneLedger()).isFalse();
        assertThat(dto.isIntegridad()).isTrue();
        assertThat(dto.getEventos()).isEmpty();
    }

    @Test
    void verificarVariante_cadenaCorrecta_marcaEventoVerificado() {
        ProductoVariante variante = variante(1);
        EventoLedger evento = genesis(variante);
        when(eventoRepository.findByVarianteIdVarianteOrderBySecuenciaAsc(20L))
            .thenReturn(List.of(evento));

        VarianteLedgerDTO dto = ledgerService.verificarVariante(variante);

        assertThat(dto.isTieneLedger()).isTrue();
        assertThat(dto.isIntegridad()).isTrue();
        assertThat(dto.getEventos()).hasSize(1);
        assertThat(dto.getEventos().get(0).isVerificado()).isTrue();
        assertThat(dto.getEventos().get(0).getNombreOrigen()).isEqualTo("artista");
    }

    @Test
    void verificarVariante_cadenaAlterada_marcaEventoNoVerificado() {
        ProductoVariante variante = variante(1);
        EventoLedger evento = genesis(variante);
        evento.setHashEvento("0".repeat(64));
        when(eventoRepository.findByVarianteIdVarianteOrderBySecuenciaAsc(20L))
            .thenReturn(List.of(evento));

        VarianteLedgerDTO dto = ledgerService.verificarVariante(variante);

        assertThat(dto.isIntegridad()).isFalse();
        assertThat(dto.getEventos().get(0).isVerificado()).isFalse();
        assertThat(dto.getMensaje()).contains("no coincide");
    }

    @Test
    void verificarProducto_agregaLasVariantesYLaIntegridadGlobal() {
        ProductoVariante variante = variante(1);
        Producto producto = variante.getProducto();
        EventoLedger evento = genesis(variante);
        when(eventoRepository.findByVarianteIdVarianteOrderBySecuenciaAsc(20L))
            .thenReturn(List.of(evento));

        ProductoLedgerDTO dto = ledgerService.verificarProducto(producto);

        assertThat(dto.isIntegridad()).isTrue();
        assertThat(dto.getVariantes()).hasSize(1);
        assertThat(dto.getVariantes().get(0).getEventos()).hasSize(1);
    }

    private EventoLedger genesis(ProductoVariante variante) {
        when(eventoRepository.findTopByVarianteIdVarianteOrderBySecuenciaDesc(20L))
            .thenReturn(Optional.empty());
        when(eventoRepository.save(any(EventoLedger.class)))
            .thenAnswer(inv -> {
                EventoLedger evento = inv.getArgument(0);
                evento.setIdEventoLedger(1L);
                return evento;
            });
        return ledgerService.registrarGenesis(variante, "hashTerminos");
    }

    private ProductoVariante variante(int stock) {
        Usuario artista = Usuario.builder()
            .idUsuario(5L)
            .nombreUsuario("artista")
            .build();
        Moneda moneda = Moneda.builder()
            .idMoneda(3L)
            .codigo("ARS")
            .nombre("Peso")
            .build();

        Producto producto = new Producto();
        producto.setIdProducto(10L);
        producto.setTitulo("Plancha de otoño");
        producto.setArtista(artista);
        producto.setMoneda(moneda);

        ProductoVariante variante = new ProductoVariante();
        variante.setIdVariante(20L);
        variante.setProducto(producto);
        variante.setNombre("Única");
        variante.setStockDisponible(stock);
        variante.setSha256(SHA);
        variante.setPropietarioActual(artista);
        producto.getVariantes().add(variante);
        return variante;
    }
}
