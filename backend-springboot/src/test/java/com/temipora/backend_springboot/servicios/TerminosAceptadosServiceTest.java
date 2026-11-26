package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.AceptacionTerminosPlataforma;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.Terminos;
import com.temipora.backend_springboot.modelo.TerminosAceptadosDTO;
import com.temipora.backend_springboot.modelo.TerminosPlataforma;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;

@ExtendWith(MockitoExtension.class)
class TerminosAceptadosServiceTest {

    private static final String SNAPSHOT = """
        {"idTerminos":3,"idServicio":9,"version":2,
         "informacionGeneral":"Condiciones generales",
         "politicaRevisiones":"Dos revisiones",
         "politicaCancelaciones":"Con seña",
         "usoArtista":"Uso del artista",
         "usoCliente":"Uso del cliente",
         "pagoExtra":"Pago extra",
         "queDibuja":"Personajes",
         "queNoDibuja":"Contenido ilegal",
         "derechos":[]}
        """;

    @Mock private SolicitudRepository solicitudRepository;
    @Mock private PedidoRepository pedidoRepository;

    private TerminosAceptadosService service;

    @BeforeEach
    void setUp() {
        service = new TerminosAceptadosService(
            solicitudRepository, pedidoRepository, "https://temipora.test/");
    }

    @Test
    void obtenerConPedidoYPlataformaDevuelveTodosLosDatos() {
        Solicitud solicitud = solicitud(7L, SNAPSHOT);
        when(solicitudRepository.findByIdSolicitud(7L))
            .thenReturn(Optional.of(solicitud));
        AceptacionTerminosPlataforma aceptacion = aceptacion(
            LocalDateTime.of(2026, 11, 1, 10, 0));
        Pedido pedido = new Pedido();
        pedido.setIdPedido(41L);
        pedido.setAceptacionTerminosPlataforma(aceptacion);
        when(pedidoRepository.findBySolicitudIdSolicitud(7L))
            .thenReturn(Optional.of(pedido));

        TerminosAceptadosDTO dto = service.obtener(
            CodigoTerminos.codificar(7L, HuellaTexto.sha256(SNAPSHOT)));

        assertThat(dto.getIdPedido()).isEqualTo(41L);
        assertThat(dto.getCliente()).isEqualTo("cliente");
        assertThat(dto.getArtista()).isEqualTo("artista");
        assertThat(dto.getServicio()).isEqualTo("Ilustración");
        assertThat(dto.getVersion()).isEqualTo(2);
        assertThat(dto.isDelServicio()).isTrue();
        assertThat(dto.getAceptadosEn())
            .isEqualTo(LocalDateTime.of(2026, 10, 20, 12, 30));
        assertThat(dto.getPlataformaVersion()).isEqualTo(5);
        assertThat(dto.getPlataformaTitulo()).isEqualTo("Términos 2026");
        assertThat(dto.getPlataformaAceptadosEn())
            .isEqualTo(LocalDateTime.of(2026, 11, 1, 10, 0));
        assertThat(dto.getPlataformaHuella())
            .isEqualTo(HuellaTexto.sha256("{\"version\":5}"));
        assertThat(dto.getUrlVerificacion())
            .startsWith("https://temipora.test/terminos-aceptados/TA-")
            .doesNotContain("//terminos");
        assertThat(dto.getContenido()).isNotNull();
        assertThat(dto.getContenido().informacionGeneral())
            .isEqualTo("Condiciones generales");
        assertThat(dto.getContenido().derechos()).isEmpty();
    }

    @Test
    void obtenerSinPedidoNiTerminosDeServicioCompletaLosNulos() {
        Solicitud solicitud = solicitud(8L, SNAPSHOT);
        solicitud.setTerminos(null);
        when(solicitudRepository.findByIdSolicitud(8L))
            .thenReturn(Optional.of(solicitud));
        when(pedidoRepository.findBySolicitudIdSolicitud(8L))
            .thenReturn(Optional.empty());

        TerminosAceptadosDTO dto = service.obtener(
            CodigoTerminos.codificar(8L, HuellaTexto.sha256(SNAPSHOT)));

        assertThat(dto.getIdPedido()).isNull();
        assertThat(dto.isDelServicio()).isFalse();
        assertThat(dto.getPlataformaVersion()).isNull();
        assertThat(dto.getPlataformaTitulo()).isNull();
        assertThat(dto.getPlataformaAceptadosEn()).isNull();
        assertThat(dto.getPlataformaHuella()).isNull();
    }

    @Test
    void obtenerConCodigoInvalidoFalla() {
        assertThatThrownBy(() -> service.obtener("NO-ES-UN-CODIGO"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("aceptación de términos");
    }

    @Test
    void obtenerConSolicitudInexistenteFalla() {
        String codigo = CodigoTerminos.codificar(
            99L, HuellaTexto.sha256(SNAPSHOT));
        when(solicitudRepository.findByIdSolicitud(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(codigo))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void obtenerConCodigoDeOtraHuellaFalla() {
        String otroSnapshot = "otro contenido";
        String codigo = CodigoTerminos.codificar(
            7L, HuellaTexto.sha256(otroSnapshot));
        when(solicitudRepository.findByIdSolicitud(7L))
            .thenReturn(Optional.of(solicitud(7L, SNAPSHOT)));

        assertThatThrownBy(() -> service.obtener(codigo))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void obtenerConSnapshotJsonInvalidoDevuelveContenidoNulo() {
        Solicitud solicitud = solicitud(7L, "{esto no es json");
        when(solicitudRepository.findByIdSolicitud(7L))
            .thenReturn(Optional.of(solicitud));
        when(pedidoRepository.findBySolicitudIdSolicitud(7L))
            .thenReturn(Optional.empty());

        TerminosAceptadosDTO dto = service.obtener(
            CodigoTerminos.codificar(
                7L, HuellaTexto.sha256("{esto no es json")));

        assertThat(dto.getContenido()).isNull();
    }

    @Test
    void obtenerConSnapshotVacioDevuelveContenidoNulo() {
        Solicitud solicitud = solicitud(7L, "   ");
        when(solicitudRepository.findByIdSolicitud(7L))
            .thenReturn(Optional.of(solicitud));
        when(pedidoRepository.findBySolicitudIdSolicitud(7L))
            .thenReturn(Optional.empty());

        TerminosAceptadosDTO dto = service.obtener(
            CodigoTerminos.codificar(7L, HuellaTexto.sha256("   ")));

        assertThat(dto.getContenido()).isNull();
        assertThat(dto.getHuella()).isEqualTo(HuellaTexto.sha256("   "));
    }

    private static Solicitud solicitud(Long idSolicitud, String snapshot) {
        Usuario cliente = new Usuario();
        cliente.setNombreUsuario("cliente");
        Usuario artista = new Usuario();
        artista.setNombreUsuario("artista");
        Servicio servicio = new Servicio();
        servicio.setNombre("Ilustración");
        Terminos terminos = new Terminos();
        terminos.setServicio(servicio);

        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(idSolicitud);
        solicitud.setCliente(cliente);
        solicitud.setArtista(artista);
        solicitud.setServicio(servicio);
        solicitud.setTerminos(terminos);
        solicitud.setTerminosVersion(2);
        solicitud.setTerminosAceptadosEn(
            LocalDateTime.of(2026, 10, 20, 12, 30));
        solicitud.setTerminosSnapshot(snapshot);
        return solicitud;
    }

    private static AceptacionTerminosPlataforma aceptacion(
            LocalDateTime fecha) {
        TerminosPlataforma plataforma = new TerminosPlataforma();
        plataforma.setTitulo("Términos 2026");
        AceptacionTerminosPlataforma aceptacion =
            new AceptacionTerminosPlataforma();
        aceptacion.setVersion(5);
        aceptacion.setSnapshot("{\"version\":5}");
        aceptacion.setFechaAceptacion(fecha);
        aceptacion.setTerminosPlataforma(plataforma);
        return aceptacion;
    }
}
