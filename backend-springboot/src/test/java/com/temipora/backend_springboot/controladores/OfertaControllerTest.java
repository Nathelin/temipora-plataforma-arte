package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

import com.temipora.backend_springboot.modelo.CampanaReactivacion;
import com.temipora.backend_springboot.modelo.ConfiguracionComision;
import com.temipora.backend_springboot.modelo.EstadoCampanaReactivacion;
import com.temipora.backend_springboot.modelo.EstadoOfertaReactivacion;
import com.temipora.backend_springboot.modelo.OfertaReactivacion;
import com.temipora.backend_springboot.modelo.OfertaReactivacionDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.ComisionService;
import com.temipora.backend_springboot.servicios.OfertaReactivacionService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OfertaControllerTest {

    @Mock
    private OfertaReactivacionService ofertaService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private ComisionService comisionService;

    private OfertaController controller;

    @BeforeEach
    void setUp() {
        controller = new OfertaController(ofertaService, usuarioService, comisionService);
        lenient().when(usuarioService.getByNombreUsuario("u"))
            .thenReturn(Usuario.builder().idUsuario(1L).nombreUsuario("u").build());
        lenient().when(comisionService.obtener()).thenReturn(comision());
    }

    @Test
    void misOfertas_mapeaLasOfertasActivas() {
        when(ofertaService.activasDeUsuario(1L)).thenReturn(List.of(oferta()));

        List<OfertaReactivacionDTO> resultado =
            controller.misOfertas(auth());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdOferta()).isEqualTo(5L);
    }

    @Test
    void campana_mapeaLaCampanaPorToken() {
        when(ofertaService.campanaPorToken("tok")).thenReturn(campana());

        assertThat(controller.campana("tok").getToken()).isEqualTo("tok");
    }

    @Test
    void oferta_mapeaLaOfertaDelUsuario() {
        when(ofertaService.ofertaDeUsuario(5L, 1L)).thenReturn(oferta());

        assertThat(controller.oferta(5L, auth()).getIdOferta()).isEqualTo(5L);
        verify(ofertaService).ofertaDeUsuario(5L, 1L);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Authentication auth() {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("u");
        return authentication;
    }

    private ConfiguracionComision comision() {
        ConfiguracionComision comision = new ConfiguracionComision();
        comision.setPorcentajeServicio(new BigDecimal("10.00"));
        return comision;
    }

    private OfertaReactivacion oferta() {
        OfertaReactivacion oferta = new OfertaReactivacion();
        oferta.setIdOferta(5L);
        oferta.setPorcentajeDescuento(new BigDecimal("10.00"));
        oferta.setEstado(EstadoOfertaReactivacion.OFRECIDA);
        oferta.setFechaVencimiento(LocalDateTime.now().plusDays(1));
        return oferta;
    }

    private CampanaReactivacion campana() {
        CampanaReactivacion campana = new CampanaReactivacion();
        campana.setIdCampana(1L);
        campana.setToken("tok");
        campana.setEstado(EstadoCampanaReactivacion.ACTIVA);
        campana.setFechaVencimiento(LocalDateTime.now().plusDays(1));
        return campana;
    }
}
