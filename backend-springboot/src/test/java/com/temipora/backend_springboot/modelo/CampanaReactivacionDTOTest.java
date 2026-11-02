package com.temipora.backend_springboot.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CampanaReactivacionDTOTest {

    private final LocalDateTime ahora = LocalDateTime.of(2026, 5, 1, 12, 0);

    @Test
    void desdeEntidad_vigente_mapeaUsuarioYOfertasOrdenadas() {
        CampanaReactivacion campana = campana(EstadoCampanaReactivacion.ACTIVA,
            ahora.plusDays(1));
        campana.setUsuario(Usuario.builder().idUsuario(9L).nombreUsuario("cosmiq").build());
        campana.agregarOferta(oferta(2L));
        campana.agregarOferta(oferta(1L));

        CampanaReactivacionDTO dto = CampanaReactivacionDTO.desdeEntidad(campana, ahora);

        assertThat(dto.getIdCampana()).isEqualTo(7L);
        assertThat(dto.getToken()).isEqualTo("token");
        assertThat(dto.getNombreUsuario()).isEqualTo("cosmiq");
        assertThat(dto.getEstado()).isEqualTo("ACTIVA");
        assertThat(dto.isVigente()).isTrue();
        assertThat(dto.getOfertas()).extracting(OfertaReactivacionDTO::getIdOferta)
            .containsExactly(1L, 2L);
    }

    @Test
    void desdeEntidad_vencida_noEsVigente() {
        CampanaReactivacion campana = campana(EstadoCampanaReactivacion.ACTIVA,
            ahora.minusDays(1));

        assertThat(CampanaReactivacionDTO.desdeEntidad(campana, ahora).isVigente())
            .isFalse();
    }

    @Test
    void desdeEntidad_estadoNoActivo_noEsVigente() {
        CampanaReactivacion campana = campana(EstadoCampanaReactivacion.CANCELADA,
            ahora.plusDays(1));

        assertThat(CampanaReactivacionDTO.desdeEntidad(campana, ahora).isVigente())
            .isFalse();
    }

    @Test
    void desdeEntidad_sinUsuarioNiOfertas_dejaNulosYListaVacia() {
        CampanaReactivacion campana = campana(EstadoCampanaReactivacion.ACTIVA,
            ahora.plusDays(1));
        campana.setOfertas(null);

        CampanaReactivacionDTO dto = CampanaReactivacionDTO.desdeEntidad(campana, ahora);

        assertThat(dto.getNombreUsuario()).isNull();
        assertThat(dto.getOfertas()).isEmpty();
    }

    @Test
    void desdeEntidad_estadoNulo_mapeaEstadoNulo() {
        CampanaReactivacion campana = campana(null, ahora.plusDays(1));

        assertThat(CampanaReactivacionDTO.desdeEntidad(campana, ahora).getEstado())
            .isNull();
    }

    @Test
    void desdeEntidad_conComisionLaPropaga() {
        CampanaReactivacion campana = campana(EstadoCampanaReactivacion.ACTIVA,
            ahora.plusDays(1));
        campana.agregarOferta(oferta(1L));

        CampanaReactivacionDTO dto = CampanaReactivacionDTO.desdeEntidad(
            campana, ahora, new BigDecimal("8.00"));

        assertThat(dto.getOfertas().get(0).getPorcentajeDescuento())
            .isEqualByComparingTo("8.00");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private CampanaReactivacion campana(
            EstadoCampanaReactivacion estado, LocalDateTime vencimiento) {
        CampanaReactivacion campana = new CampanaReactivacion();
        campana.setIdCampana(7L);
        campana.setToken("token");
        campana.setEstado(estado);
        campana.setFechaCreacion(ahora.minusDays(1));
        campana.setFechaVencimiento(vencimiento);
        return campana;
    }

    private OfertaReactivacion oferta(Long id) {
        OfertaReactivacion oferta = new OfertaReactivacion();
        oferta.setIdOferta(id);
        oferta.setPorcentajeDescuento(new BigDecimal("10.00"));
        oferta.setEstado(EstadoOfertaReactivacion.OFRECIDA);
        oferta.setFechaVencimiento(ahora.plusDays(1));
        return oferta;
    }
}
