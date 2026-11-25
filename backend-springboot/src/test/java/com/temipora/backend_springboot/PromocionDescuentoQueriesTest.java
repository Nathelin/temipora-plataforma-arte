package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import com.temipora.backend_springboot.modelo.EstadoOfertaReactivacion;
import com.temipora.backend_springboot.modelo.InformeDescuentosDTO;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.repositorios.CampanaReactivacionRepository;
import com.temipora.backend_springboot.repositorios.OfertaReactivacionRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.servicios.PromocionDescuentoService;

@SpringBootTest
class PromocionDescuentoQueriesTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private OfertaReactivacionRepository ofertaRepository;

    @Autowired
    private CampanaReactivacionRepository campanaRepository;

    @Autowired
    private PromocionDescuentoService service;

    @Test
    void consultasDelInforme_ejecutanSinErrores() {
        LocalDateTime hasta = LocalDateTime.now();
        LocalDateTime desde = hasta.minusDays(30);

        var pagina = pedidoRepository.buscarConDescuento(
            desde, hasta, "", false, PageRequest.of(0, 20));

        assertThat(pagina).isNotNull();
        assertThat(pagina.getContent()).isNotNull();
        assertThat(pedidoRepository.resumirConDescuento(
            desde, hasta, "", false)).isNotNull();
        assertThat(pedidoRepository.contarPedidosPeriodo(
            desde, hasta, "")).isGreaterThanOrEqualTo(0);
        assertThat(ofertaRepository.contarPorEstadoEntre(
            desde, hasta)).isNotNull();
        assertThat(campanaRepository.contarEntre(
            desde, hasta)).isGreaterThanOrEqualTo(0);
        assertThat(ofertaRepository.existsByCampanaIdCampanaAndEstado(
            -1L, EstadoOfertaReactivacion.OFRECIDA)).isFalse();
    }

    @Test
    void informe_seGeneraConElPeriodoPedido() {
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.minusWeeks(3);

        InformeDescuentosDTO informe = service.generar(
            desde.toString(), hasta.toString(), null, false, 0, 20);

        assertThat(informe.desde()).isEqualTo(desde);
        assertThat(informe.hasta()).isEqualTo(hasta);
        assertThat(informe.resumen()).isNotNull();
        assertThat(informe.ofertas()).isNotNull();
        assertThat(informe.pedidos()).isNotNull();
    }
}
