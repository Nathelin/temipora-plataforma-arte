package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.CampanaReactivacion;
import com.temipora.backend_springboot.modelo.EstadoCampanaReactivacion;
import com.temipora.backend_springboot.modelo.EstadoOfertaReactivacion;
import com.temipora.backend_springboot.modelo.OfertaReactivacion;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.repositorios.CampanaReactivacionRepository;
import com.temipora.backend_springboot.repositorios.OfertaReactivacionRepository;

@ExtendWith(MockitoExtension.class)
class OfertaReactivacionServiceTest {

    @Mock
    private OfertaReactivacionRepository ofertaRepository;

    @Mock
    private CampanaReactivacionRepository campanaRepository;

    @Mock
    private ComisionService comisionService;

    @InjectMocks
    private OfertaReactivacionService service;

    @Test
    void marcarUtilizada_registraElMomentoDeUso() {
        OfertaReactivacion oferta = new OfertaReactivacion();

        service.marcarUtilizada(oferta);

        assertThat(oferta.getEstado())
            .isEqualTo(EstadoOfertaReactivacion.UTILIZADA);
        assertThat(oferta.getFechaUtilizada()).isNotNull();
    }

    @Test
    void cerrarCampanasVencidas_distingueUsadasDeVencidas() {
        CampanaReactivacion usada = new CampanaReactivacion();
        usada.setIdCampana(1L);
        CampanaReactivacion sinUsar = new CampanaReactivacion();
        sinUsar.setIdCampana(2L);

        when(campanaRepository.findByEstadoAndFechaVencimientoBefore(
                eq(EstadoCampanaReactivacion.ACTIVA), any()))
            .thenReturn(List.of(usada, sinUsar));
        when(ofertaRepository.existsByCampanaIdCampanaAndEstado(
                1L, EstadoOfertaReactivacion.UTILIZADA))
            .thenReturn(true);
        when(ofertaRepository.existsByCampanaIdCampanaAndEstado(
                2L, EstadoOfertaReactivacion.UTILIZADA))
            .thenReturn(false);

        assertThat(service.cerrarCampanasVencidas()).isEqualTo(2);
        assertThat(usada.getEstado())
            .isEqualTo(EstadoCampanaReactivacion.UTILIZADA);
        assertThat(sinUsar.getEstado())
            .isEqualTo(EstadoCampanaReactivacion.VENCIDA);
    }

    @Test
    void activasDeUsuario_muestraUnaSolaOfertaPorServicio() {
        CampanaReactivacion campana = new CampanaReactivacion();
        campana.setEstado(EstadoCampanaReactivacion.ACTIVA);
        campana.setFechaVencimiento(LocalDateTime.now().plusDays(2));

        OfertaReactivacion primera = oferta(1L, 10L, campana);
        OfertaReactivacion duplicada = oferta(2L, 10L, campana);
        OfertaReactivacion otroServicio = oferta(3L, 11L, campana);

        when(ofertaRepository.findActivasPorUsuario(eq(7L), any(), any()))
            .thenReturn(List.of(duplicada, otroServicio, primera));

        List<OfertaReactivacion> resultado = service.activasDeUsuario(7L);

        assertThat(resultado)
            .extracting(OfertaReactivacion::getIdOferta)
            .containsExactly(1L, 3L);
    }

    private OfertaReactivacion oferta(
            Long idOferta,
            Long idServicio,
            CampanaReactivacion campana) {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(idServicio);

        OfertaReactivacion oferta = new OfertaReactivacion();
        oferta.setIdOferta(idOferta);
        oferta.setServicio(servicio);
        oferta.setCampana(campana);
        oferta.setEstado(EstadoOfertaReactivacion.OFRECIDA);
        oferta.setFechaVencimiento(LocalDateTime.now().plusDays(2));
        return oferta;
    }
}
