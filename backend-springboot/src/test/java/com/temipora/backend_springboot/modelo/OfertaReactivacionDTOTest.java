package com.temipora.backend_springboot.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class OfertaReactivacionDTOTest {

    private final LocalDateTime ahora = LocalDateTime.of(2026, 5, 1, 12, 0);

    @Test
    void desdeEntidad_completa_marcaVigenteYClampaElDescuento() {
        OfertaReactivacion oferta = oferta(1L, EstadoOfertaReactivacion.OFRECIDA,
            new BigDecimal("15.00"), ahora.plusDays(1));

        OfertaReactivacionDTO dto = OfertaReactivacionDTO.desdeEntidad(
            oferta, ahora, new BigDecimal("10.00"));

        assertThat(dto.getIdOferta()).isEqualTo(1L);
        assertThat(dto.getIdCampana()).isEqualTo(7L);
        assertThat(dto.getTokenCampana()).isEqualTo("token");
        assertThat(dto.getIdServicio()).isEqualTo(4L);
        assertThat(dto.getNombreServicio()).isEqualTo("Retrato");
        assertThat(dto.getDescripcionServicio()).isEqualTo("desc");
        assertThat(dto.getIdArtista()).isEqualTo(3L);
        assertThat(dto.getNombreArtista()).isEqualTo("artista");
        assertThat(dto.getUrlFotoArtista()).isEqualTo("foto.png");
        assertThat(dto.getImagen()).isEqualTo("portada.png");
        assertThat(dto.getMoneda()).isEqualTo("ARS");
        assertThat(dto.getEstado()).isEqualTo("OFRECIDA");
        assertThat(dto.getPorcentajeDescuento()).isEqualByComparingTo("10.00");
        assertThat(dto.isVigente()).isTrue();
    }

    @Test
    void desdeEntidad_sinComision_conservaElDescuentoOriginal() {
        OfertaReactivacion oferta = oferta(1L, EstadoOfertaReactivacion.OFRECIDA,
            new BigDecimal("15.00"), ahora.plusDays(1));

        OfertaReactivacionDTO dto = OfertaReactivacionDTO.desdeEntidad(oferta, ahora);

        assertThat(dto.getPorcentajeDescuento()).isEqualByComparingTo("15.00");
    }

    @Test
    void desdeEntidad_descuentoNegativoConComision_quedaEnCero() {
        OfertaReactivacion oferta = oferta(1L, EstadoOfertaReactivacion.OFRECIDA,
            new BigDecimal("-5.00"), ahora.plusDays(1));

        OfertaReactivacionDTO dto = OfertaReactivacionDTO.desdeEntidad(
            oferta, ahora, new BigDecimal("10.00"));

        assertThat(dto.getPorcentajeDescuento()).isEqualByComparingTo("0");
    }

    @Test
    void desdeEntidad_estadoNoVigente_oVencida_noMarcaVigente() {
        OfertaReactivacion utilizada = oferta(1L, EstadoOfertaReactivacion.UTILIZADA,
            new BigDecimal("10.00"), ahora.plusDays(1));
        OfertaReactivacion vencida = oferta(1L, EstadoOfertaReactivacion.OFRECIDA,
            new BigDecimal("10.00"), ahora.minusDays(1));

        assertThat(OfertaReactivacionDTO.desdeEntidad(utilizada, ahora).isVigente())
            .isFalse();
        assertThat(OfertaReactivacionDTO.desdeEntidad(vencida, ahora).isVigente())
            .isFalse();
    }

    @Test
    void desdeEntidad_artistaSeTomaDelServicioSiNoHayExplicito() {
        OfertaReactivacion oferta = oferta(1L, EstadoOfertaReactivacion.OFRECIDA,
            new BigDecimal("10.00"), ahora.plusDays(1));
        oferta.setArtista(null);

        OfertaReactivacionDTO dto = OfertaReactivacionDTO.desdeEntidad(oferta, ahora);

        assertThat(dto.getIdArtista()).isEqualTo(3L);
        assertThat(dto.getNombreArtista()).isEqualTo("artista");
    }

    @Test
    void desdeEntidad_sinServicioNiCampana_dejaCamposNulos() {
        OfertaReactivacion oferta = new OfertaReactivacion();
        oferta.setIdOferta(1L);
        oferta.setEstado(EstadoOfertaReactivacion.OFRECIDA);

        OfertaReactivacionDTO dto = OfertaReactivacionDTO.desdeEntidad(oferta, ahora);

        assertThat(dto.getNombreServicio()).isNull();
        assertThat(dto.getNombreArtista()).isNull();
        assertThat(dto.getIdCampana()).isNull();
        assertThat(dto.isVigente()).isFalse();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private OfertaReactivacion oferta(
            Long idOferta,
            EstadoOfertaReactivacion estado,
            BigDecimal descuento,
            LocalDateTime vencimiento) {
        Usuario artista = Usuario.builder()
            .idUsuario(3L)
            .nombreUsuario("artista")
            .urlFotoPerfil("foto.png")
            .build();

        Servicio servicio = new Servicio();
        servicio.setIdServicio(4L);
        servicio.setNombre("Retrato");
        servicio.setDescripcion("desc");
        servicio.setPrecioBase(new BigDecimal("100.00"));
        servicio.setImagenPortada("portada.png");
        servicio.setMoneda(Moneda.builder().codigo("ARS").build());
        servicio.setArtista(artista);

        CampanaReactivacion campana = new CampanaReactivacion();
        campana.setIdCampana(7L);
        campana.setToken("token");
        campana.setEstado(EstadoCampanaReactivacion.ACTIVA);
        campana.setFechaVencimiento(ahora.plusDays(2));

        OfertaReactivacion oferta = new OfertaReactivacion();
        oferta.setIdOferta(idOferta);
        oferta.setCampana(campana);
        oferta.setServicio(servicio);
        oferta.setArtista(artista);
        oferta.setPorcentajeDescuento(descuento);
        oferta.setEstado(estado);
        oferta.setFechaVencimiento(vencimiento);
        oferta.setMotivo("motivo");
        return oferta;
    }
}
