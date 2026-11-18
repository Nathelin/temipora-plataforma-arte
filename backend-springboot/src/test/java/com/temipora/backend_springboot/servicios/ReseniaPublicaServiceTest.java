package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.temipora.backend_springboot.modelo.ConfiguracionResenia;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoResenia;
import com.temipora.backend_springboot.modelo.EtiquetaReputacion;
import com.temipora.backend_springboot.modelo.PaginaReseniasDTO;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.PromedioReseniasDTO;
import com.temipora.backend_springboot.modelo.ReputacionClienteDTO;
import com.temipora.backend_springboot.modelo.Resenia;
import com.temipora.backend_springboot.modelo.ReseniaPublicaResumenDTO;
import com.temipora.backend_springboot.modelo.ReseniaVersion;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ReseniaOpcionRepository;
import com.temipora.backend_springboot.repositorios.ReseniaRepository;
import com.temipora.backend_springboot.repositorios.ReseniaValorRepository;
import com.temipora.backend_springboot.repositorios.ReseniaVersionRepository;

@ExtendWith(MockitoExtension.class)
class ReseniaPublicaServiceTest {

    @Mock
    private ReseniaRepository reseniaRepository;
    @Mock
    private ReseniaVersionRepository versionRepository;
    @Mock
    private ReseniaValorRepository valorRepository;
    @Mock
    private ReseniaOpcionRepository opcionRepository;
    @Mock
    private ConfiguracionReseniaService configuracionService;

    @InjectMocks
    private ReseniaPublicaService reseniaPublicaService;

    @Test
    void resumenPorPedidos_anonima_ocultaElAutor() {
        Pedido pedido = pedido(false);
        Resenia resenia = resenia(pedido, false);
        stubResumenPorPedidos(resenia, version(100L, 4, "Muy bueno"));

        Map<Long, ReseniaPublicaResumenDTO> mapa =
            reseniaPublicaService.resumenPorPedidos(List.of(10L));

        assertThat(mapa.get(10L).isAnonima()).isTrue();
        assertThat(mapa.get(10L).getAutorMostrado()).isEqualTo("Cliente verificado");
    }

    @Test
    void resumenPorPedidos_visible_muestraElAutor() {
        Pedido pedido = pedido(false);
        Resenia resenia = resenia(pedido, true);
        stubResumenPorPedidos(resenia, version(100L, 5, "Excelente"));

        Map<Long, ReseniaPublicaResumenDTO> mapa =
            reseniaPublicaService.resumenPorPedidos(List.of(10L));

        assertThat(mapa.get(10L).isAnonima()).isFalse();
        assertThat(mapa.get(10L).getAutorMostrado()).isEqualTo("Cliente");
    }

    @Test
    void resumenPorPedidos_servicioSensibleRespetaElNombreElegido() {
        Pedido pedido = pedido(true);
        Resenia resenia = resenia(pedido, true);
        stubResumenPorPedidos(resenia, version(100L, 4, "Reservado"));

        Map<Long, ReseniaPublicaResumenDTO> mapa =
            reseniaPublicaService.resumenPorPedidos(List.of(10L));

        assertThat(mapa.get(10L).isAnonima()).isFalse();
        assertThat(mapa.get(10L).getAutorMostrado()).isEqualTo("Cliente");
    }

    @Test
    void resumenPorPedidos_servicioSensibleSinNombre_sigueAnonima() {
        Pedido pedido = pedido(true);
        Resenia resenia = resenia(pedido, false);
        stubResumenPorPedidos(resenia, version(100L, 4, "Reservado"));

        Map<Long, ReseniaPublicaResumenDTO> mapa =
            reseniaPublicaService.resumenPorPedidos(List.of(10L));

        assertThat(mapa.get(10L).isAnonima()).isTrue();
        assertThat(mapa.get(10L).getAutorMostrado()).isEqualTo("Cliente verificado");
    }

    @Test
    void listarServicio_mapeaLaVersionActualYElResumen() {
        Pedido pedido = pedido(false);
        Resenia resenia = resenia(pedido, true);
        ReseniaVersion version = version(100L, 4, "Muy bueno");
        version.setResenia(resenia);
        Page<Resenia> page = new PageImpl<>(
            List.of(resenia), PageRequest.of(0, 10), 1);

        when(reseniaRepository.findPublicasPorServicio(
            eq(1L), isNull(), isNull(), eq("RECIENTES"),
            eq(EstadoResenia.PUBLICADA), eq(EstadoPedido.FINALIZADO),
            any(Pageable.class))).thenReturn(page);
        when(reseniaRepository.resumenConteoPromedio(
            eq(1L), isNull(), eq(EstadoResenia.PUBLICADA),
            eq(EstadoPedido.FINALIZADO)))
            .thenReturn(List.<Object[]>of(new Object[] { 1L, 4.0 }));
        when(reseniaRepository.distribucionPublica(
            eq(1L), isNull(), eq(EstadoResenia.PUBLICADA),
            eq(EstadoPedido.FINALIZADO))).thenReturn(List.of());
        when(versionRepository.findActualesPorReseniaIds(List.of(1L)))
            .thenReturn(List.of(version));
        when(valorRepository.findByVersionIdVersionIn(List.of(100L)))
            .thenReturn(List.of());
        when(opcionRepository.findByVersionIdVersionIn(List.of(100L)))
            .thenReturn(List.of());

        PaginaReseniasDTO pagina =
            reseniaPublicaService.listarServicio(1L, null, null, "RECIENTES", 0, 10);

        assertThat(pagina.getResumen().getCantidad()).isEqualTo(1);
        assertThat(pagina.getResumen().getPromedio())
            .isEqualByComparingTo("4.00");
        assertThat(pagina.getPagina().getContenido()).hasSize(1);
        assertThat(pagina.getPagina().getContenido().get(0).getDescripcion())
            .isEqualTo("Muy bueno");
        assertThat(pagina.getPagina().getContenido().get(0).isAnonima())
            .isFalse();
    }

    @Test
    void reputacionPorClientes_aplicaUmbrales() {
        when(configuracionService.obtener()).thenReturn(configuracion());
        when(reseniaRepository.reputacionPorClientes(
            List.of(5L), EstadoResenia.PUBLICADA))
            .thenReturn(List.<Object[]>of(new Object[] { 5L, 3L, 4.7 }));

        Map<Long, ReputacionClienteDTO> mapa =
            reseniaPublicaService.reputacionPorClientes(List.of(5L));

        assertThat(mapa.get(5L).getEtiqueta())
            .isEqualTo(EtiquetaReputacion.EXCELENTE);
        assertThat(mapa.get(5L).getPromedio())
            .isEqualByComparingTo("4.70");
    }

    @Test
    void promediosPorServicios_mapeaCantidadYPromedio() {
        when(reseniaRepository.promediosPorServicios(
            List.of(1L, 2L), EstadoResenia.PUBLICADA,
            EstadoPedido.FINALIZADO))
            .thenReturn(List.of(
                new Object[] { 1L, 2L, 4.5 },
                new Object[] { 2L, 1L, 3.0 }));

        Map<Long, PromedioReseniasDTO> mapa =
            reseniaPublicaService.promediosPorServicios(List.of(1L, 2L));

        assertThat(mapa.get(1L).getCantidad()).isEqualTo(2);
        assertThat(mapa.get(1L).getPromedio()).isEqualByComparingTo("4.50");
        assertThat(mapa.get(2L).getPromedio()).isEqualByComparingTo("3.00");
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private void stubResumenPorPedidos(Resenia resenia, ReseniaVersion version) {
        version.setResenia(resenia);
        when(reseniaRepository.findPublicasPorPedidos(
            List.of(10L), EstadoResenia.PUBLICADA, EstadoPedido.FINALIZADO))
            .thenReturn(List.of(resenia));
        when(versionRepository.findActualesPorReseniaIds(List.of(1L)))
            .thenReturn(List.of(version));
    }

    private Resenia resenia(Pedido pedido, boolean mostrarNombre) {
        Resenia resenia = new Resenia();
        resenia.setIdResenia(1L);
        resenia.setPedido(pedido);
        resenia.setAutor(pedido.getCliente());
        resenia.setEstado(EstadoResenia.PUBLICADA);
        resenia.setMostrarNombre(mostrarNombre);
        return resenia;
    }

    private ReseniaVersion version(Long id, int calificacion, String descripcion) {
        ReseniaVersion version = new ReseniaVersion();
        version.setIdVersion(id);
        version.setNumero(1);
        version.setCalificacionGeneral(calificacion);
        version.setDescripcion(descripcion);
        version.setFechaVersion(LocalDateTime.now());
        return version;
    }

    private Pedido pedido(boolean sensible) {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(10L);
        pedido.setEstado(EstadoPedido.FINALIZADO);
        pedido.setCliente(usuario(5L, "cliente"));
        pedido.setArtista(usuario(2L, "artista"));
        Servicio servicio = new Servicio();
        servicio.setIdServicio(1L);
        servicio.setNombre("Retrato digital");
        servicio.setContenidoSensible(sensible);
        pedido.setServicio(servicio);
        return pedido;
    }

    private Usuario usuario(Long id, String nombreUsuario) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setNombreDisplay("Cliente");
        return usuario;
    }

    private ConfiguracionResenia configuracion() {
        ConfiguracionResenia config = new ConfiguracionResenia();
        config.setIdConfiguracion(ConfiguracionResenia.ID_UNICO);
        return config;
    }
}
