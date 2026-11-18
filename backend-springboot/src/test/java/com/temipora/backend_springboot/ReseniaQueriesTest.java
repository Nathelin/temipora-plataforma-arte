package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.temipora.backend_springboot.modelo.AmbitoResenia;
import com.temipora.backend_springboot.modelo.ConfiguracionResenia;
import com.temipora.backend_springboot.modelo.CriterioResenia;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EscalaCriterio;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoReporteResenia;
import com.temipora.backend_springboot.modelo.EstadoResenia;
import com.temipora.backend_springboot.modelo.OpcionCriterio;
import com.temipora.backend_springboot.modelo.PolaridadOpcion;
import com.temipora.backend_springboot.repositorios.ConfiguracionReseniaRepository;
import com.temipora.backend_springboot.repositorios.CriterioReseniaRepository;
import com.temipora.backend_springboot.repositorios.OpcionCriterioRepository;
import com.temipora.backend_springboot.repositorios.ReporteReseniaRepository;
import com.temipora.backend_springboot.repositorios.ReseniaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@SpringBootTest
class ReseniaQueriesTest {

    @Autowired
    private CriterioReseniaRepository criterioRepository;

    @Autowired
    private OpcionCriterioRepository opcionRepository;

    @Autowired
    private ReseniaRepository reseniaRepository;

    @Autowired
    private ConfiguracionReseniaRepository configuracionRepository;

    @Autowired
    private ReporteReseniaRepository reporteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void consultasDeArtistaConProductos_ejecutanSinError() {
        Page<com.temipora.backend_springboot.modelo.Resenia> pagina =
            reseniaRepository.findPublicasPorArtista(
                999999L, null, null, true, true, null, "RECIENTES",
                EstadoResenia.PUBLICADA, EstadoPedido.FINALIZADO,
                PageRequest.of(0, 5));
        assertThat(pagina).isNotNull();

        assertThat(reseniaRepository.resumenConteoPromedio(
            null, 999999L, EstadoResenia.PUBLICADA, EstadoPedido.FINALIZADO))
            .isNotNull();
        assertThat(reseniaRepository.distribucionPublica(
            null, 999999L, EstadoResenia.PUBLICADA, EstadoPedido.FINALIZADO))
            .isNotNull();
    }

    @Test
    void seeds_hayCriteriosActivosParaServicioYCliente() {
        List<CriterioResenia> servicio = criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.SERVICIO);
        List<CriterioResenia> cliente = criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.CLIENTE);

        assertThat(servicio).isNotEmpty();
        assertThat(cliente).isNotEmpty();
        assertThat(servicio).extracting(CriterioResenia::getNombre)
            .contains("Calidad de lo recibido", "Comunicación");
        assertThat(cliente).extracting(CriterioResenia::getNombre)
            .contains("Claridad del pedido");
    }

    @Test
    void seeds_elCriterioDeChipsTieneOpciones() {
        assertThat(opcionRepository
            .findByCriterioIdCriterioReseniaOrderByOrdenAscEtiquetaAsc(
                criterioDeChips().getIdCriterioResenia()))
            .hasSize(10);
    }

    @Test
    void seeds_elCriterioDeChipsTieneOpcionesPositivasYNegativas() {
        assertThat(opcionRepository
            .findByCriterioIdCriterioReseniaOrderByOrdenAscEtiquetaAsc(
                criterioDeChips().getIdCriterioResenia()))
            .extracting(OpcionCriterio::getPolaridad)
            .contains(PolaridadOpcion.POSITIVA, PolaridadOpcion.NEGATIVA);
    }

    private CriterioResenia criterioDeChips() {
        return criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.SERVICIO)
            .stream()
            .filter(criterio -> criterio.getEscala() == EscalaCriterio.OPCIONES_MULTIPLES)
            .filter(criterio -> "Aspectos destacados".equalsIgnoreCase(criterio.getNombre()))
            .findFirst()
            .orElseThrow();
    }

    @Test
    void configuracion_reseniaTieneSuFilaUnica() {
        assertThat(configuracionRepository
            .findById(ConfiguracionResenia.ID_UNICO))
            .isPresent();
    }

    @Test
    void agregados_deClienteInexistente_devuelvenCeroYNull() {
        assertThat(reseniaRepository.contarReseniasDeCliente(
            -1L, EstadoResenia.PUBLICADA)).isZero();
        assertThat(reseniaRepository.promedioReseniasDeCliente(
            -1L, EstadoResenia.PUBLICADA)).isNull();
    }

    @Test
    void busquedas_dePedidoInexistente_devuelvenVacio() {
        assertThat(reseniaRepository
            .findByPedidoIdPedidoAndEstadoOrderByFechaCreacionAsc(
                -1L, EstadoResenia.PUBLICADA))
            .isEmpty();
        assertThat(reseniaRepository
            .findByPedidoIdPedidoAndAutorIdUsuario(-1L, -1L))
            .isEmpty();
    }

    @Test
    void moderacion_consultasBaseSeEjecutanSinError() {
        PageRequest pageable = PageRequest.of(0, 10);

        Page<?> reportadas = reseniaRepository.findConReportesPendientes(
            EstadoReporteResenia.PENDIENTE, pageable);
        Page<?> ocultas = reseniaRepository.findPorEstado(
            EstadoResenia.OCULTA, pageable);

        assertThat(reportadas).isNotNull();
        assertThat(ocultas).isNotNull();
        assertThat(reporteRepository
            .findByReseniaIdReseniaInOrderByFechaCreacionAsc(List.of(-1L)))
            .isEmpty();
        assertThat(reporteRepository
            .findByReseniaIdReseniaAndEstadoOrderByFechaCreacionAsc(
                -1L, EstadoReporteResenia.PENDIENTE))
            .isEmpty();
        assertThat(reporteRepository
            .findIdsReseniasReportadasPorUsuario(-1L))
            .isEmpty();
        assertThat(reporteRepository
            .findHistorial(EstadoReporteResenia.DESCARTADO, pageable))
            .isNotNull();
        assertThat(reporteRepository
            .findHistorial(null, pageable))
            .isNotNull();
        assertThat(usuarioRepository.findByRol_Nombre(ERol.ADMIN))
            .isNotNull();
    }
}
