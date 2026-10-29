package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.temipora.backend_springboot.modelo.AmbitoResenia;
import com.temipora.backend_springboot.modelo.ConfiguracionResenia;
import com.temipora.backend_springboot.modelo.CriterioResenia;
import com.temipora.backend_springboot.modelo.EscalaCriterio;
import com.temipora.backend_springboot.modelo.EstadoResenia;
import com.temipora.backend_springboot.modelo.OpcionCriterio;
import com.temipora.backend_springboot.modelo.PolaridadOpcion;
import com.temipora.backend_springboot.repositorios.ConfiguracionReseniaRepository;
import com.temipora.backend_springboot.repositorios.CriterioReseniaRepository;
import com.temipora.backend_springboot.repositorios.OpcionCriterioRepository;
import com.temipora.backend_springboot.repositorios.ReseniaRepository;

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
}
