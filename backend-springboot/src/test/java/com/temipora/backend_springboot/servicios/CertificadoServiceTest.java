package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.AnclajeLedger;
import com.temipora.backend_springboot.modelo.CertificadoDTO;
import com.temipora.backend_springboot.modelo.EstadoAnclaje;
import com.temipora.backend_springboot.modelo.EventoLedgerDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.PruebaInclusionDTO;
import com.temipora.backend_springboot.modelo.TipoEventoLedger;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.VarianteLedgerDTO;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;

@ExtendWith(MockitoExtension.class)
class CertificadoServiceTest {

    private static final Long ID_VARIANTE = 36L;

    @Mock private ProductoVarianteRepository varianteRepository;
    @Mock private LedgerService ledgerService;
    @Mock private AnclajeService anclajeService;

    private CertificadoService service;

    @BeforeEach
    void setUp() {
        service = new CertificadoService(
            varianteRepository, ledgerService, anclajeService,
            "https://temipora.test/");
    }

    @Test
    void obtenerReuneVarianteLedgerYAnclajeConfirmado() {
        ProductoVariante variante = varianteConProducto();
        variante.setImagenUrl(null);
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        EventoLedgerDTO genesis = evento(100L, 0L, TipoEventoLedger.GENESIS);
        EventoLedgerDTO venta = evento(101L, 1L, TipoEventoLedger.VENTA);
        when(ledgerService.verificarVariante(variante))
            .thenReturn(ledger(List.of(genesis, venta)));
        AnclajeLedger pendiente = anclaje(
            300L, EstadoAnclaje.PENDIENTE, null,
            LocalDateTime.of(2026, 11, 20, 9, 0));
        AnclajeLedger confirmado = anclaje(
            200L, EstadoAnclaje.CONFIRMADO,
            LocalDateTime.of(2026, 11, 21, 8, 0),
            LocalDateTime.of(2026, 11, 20, 9, 0));
        AnclajeLedger fallido = anclaje(
            400L, EstadoAnclaje.FALLIDO, null, null);
        when(anclajeService.anclajesDeEventos(anyCollection())).thenReturn(Map.of(
            100L, pendiente, 101L, confirmado, 102L, fallido));
        PruebaInclusionDTO prueba = new PruebaInclusionDTO(
            "hoja", "raiz", List.of());
        when(anclajeService.pruebaInclusionDeEvento(100L))
            .thenReturn(Optional.of(prueba));

        CertificadoDTO dto = service.obtener(
            CodigoCertificado.codificar(ID_VARIANTE));

        assertThat(dto.getIdProducto()).isEqualTo(10L);
        assertThat(dto.getTitulo()).isEqualTo("Pieza");
        assertThat(dto.getIdVariante()).isEqualTo(ID_VARIANTE);
        assertThat(dto.getNombreVariante()).isEqualTo("Edición 1/1");
        assertThat(dto.getImagenUrl()).isEqualTo("portada.png");
        assertThat(dto.getIdArtista()).isEqualTo(5L);
        assertThat(dto.getNombreArtista()).isEqualTo("artista");
        assertThat(dto.getNombreDisplayArtista()).isEqualTo("La Artista");
        assertThat(dto.getUrlFotoArtista()).isEqualTo("miniatura.png");
        assertThat(dto.getNombrePropietarioActual()).isEqualTo("duenio");
        assertThat(dto.isPiezaUnica()).isTrue();
        assertThat(dto.isIntegridad()).isTrue();
        assertThat(dto.getMensaje()).isEqualTo("Cadena válida");
        assertThat(dto.getCodigoCertificado())
            .isEqualTo(CodigoCertificado.codificar(ID_VARIANTE));
        assertThat(dto.getUrlVerificacion())
            .isEqualTo("https://temipora.test/certificado/"
                + CodigoCertificado.codificar(ID_VARIANTE));
        assertThat(dto.getTrayectoria()).hasSize(2);
        assertThat(dto.getTrayectoria().get(1).secuencia()).isEqualTo(1L);
        assertThat(dto.getTrayectoria().get(1).tipo())
            .isEqualTo(TipoEventoLedger.VENTA);
        assertThat(dto.getEstadoAnclaje()).isEqualTo(EstadoAnclaje.CONFIRMADO);
        assertThat(dto.getHashRaiz()).isEqualTo("raiz-200");
        assertThat(dto.getHashBitcoin()).isEqualTo("btc-200");
        assertThat(dto.getBitcoinAltura()).isEqualTo(200L);
        assertThat(dto.getFechaAnclaje())
            .isEqualTo(LocalDateTime.of(2026, 11, 21, 8, 0));
        assertThat(dto.getPruebaInclusion()).isSameAs(prueba);
    }

    @Test
    void obtenerSinEventosNoConsultaAnclajes() {
        ProductoVariante variante = varianteConProducto();
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        when(ledgerService.verificarVariante(variante))
            .thenReturn(ledger(List.of()));

        CertificadoDTO dto = service.obtener(
            CodigoCertificado.codificar(ID_VARIANTE));

        assertThat(dto.getTrayectoria()).isEmpty();
        assertThat(dto.getEstadoAnclaje()).isNull();
        assertThat(dto.getPruebaInclusion()).isNull();
    }

    @Test
    void obtenerSinAnclajesDejaEstadoNulo() {
        ProductoVariante variante = varianteConProducto();
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        EventoLedgerDTO genesis = evento(100L, 0L, TipoEventoLedger.GENESIS);
        when(ledgerService.verificarVariante(variante))
            .thenReturn(ledger(List.of(genesis)));
        when(anclajeService.anclajesDeEventos(anyCollection()))
            .thenReturn(Map.of());
        when(anclajeService.pruebaInclusionDeEvento(100L))
            .thenReturn(Optional.empty());

        CertificadoDTO dto = service.obtener(
            CodigoCertificado.codificar(ID_VARIANTE));

        assertThat(dto.getEstadoAnclaje()).isNull();
        assertThat(dto.getHashRaiz()).isNull();
        assertThat(dto.getPruebaInclusion()).isNull();
    }

    @Test
    void obtenerUsaFechaDeSolicitudSiElAnclajeNoEstaConfirmado() {
        ProductoVariante variante = varianteConProducto();
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        EventoLedgerDTO genesis = evento(100L, 0L, TipoEventoLedger.GENESIS);
        when(ledgerService.verificarVariante(variante))
            .thenReturn(ledger(List.of(genesis)));
        LocalDateTime solicitud = LocalDateTime.of(2026, 11, 19, 7, 30);
        when(anclajeService.anclajesDeEventos(anyCollection())).thenReturn(Map.of(
            100L, anclaje(300L, EstadoAnclaje.PENDIENTE, null, solicitud)));
        when(anclajeService.pruebaInclusionDeEvento(100L))
            .thenReturn(Optional.empty());

        CertificadoDTO dto = service.obtener(
            CodigoCertificado.codificar(ID_VARIANTE));

        assertThat(dto.getEstadoAnclaje()).isEqualTo(EstadoAnclaje.PENDIENTE);
        assertThat(dto.getFechaAnclaje()).isEqualTo(solicitud);
    }

    @Test
    void obtenerSinProductoNiPropietarioCompletaLosNulos() {
        ProductoVariante variante = new ProductoVariante();
        variante.setIdVariante(ID_VARIANTE);
        variante.setNombre("Suelta");
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        when(ledgerService.verificarVariante(variante))
            .thenReturn(ledger(List.of()));

        CertificadoDTO dto = service.obtener(
            CodigoCertificado.codificar(ID_VARIANTE));

        assertThat(dto.getIdProducto()).isNull();
        assertThat(dto.getTitulo()).isNull();
        assertThat(dto.getImagenUrl()).isNull();
        assertThat(dto.getIdArtista()).isNull();
        assertThat(dto.getNombrePropietarioActual()).isNull();
    }

    @Test
    void obtenerUsaLaFotoDePerfilCuandoNoHayMiniatura() {
        ProductoVariante variante = varianteConProducto();
        Producto producto = variante.getProducto();
        producto.getArtista().setUrlFotoPerfilMiniatura(null);
        producto.getArtista().setUrlFotoPerfil("perfil.png");
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        when(ledgerService.verificarVariante(variante))
            .thenReturn(ledger(List.of()));

        CertificadoDTO dto = service.obtener(
            CodigoCertificado.codificar(ID_VARIANTE));

        assertThat(dto.getUrlFotoArtista()).isEqualTo("perfil.png");
    }

    @Test
    void obtenerNoMuestraAlPropietarioSiLoOculto() {
        ProductoVariante variante = varianteConProducto();
        variante.getPropietarioActual().setMostrarPropietario(false);
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        when(ledgerService.verificarVariante(variante))
            .thenReturn(ledger(List.of()));

        CertificadoDTO dto = service.obtener(
            CodigoCertificado.codificar(ID_VARIANTE));

        assertThat(dto.getNombrePropietarioActual()).isNull();
    }

    @Test
    void obtenerConCodigoInvalidoFalla() {
        assertThatThrownBy(() -> service.obtener("NO-EXISTE"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Certificado no encontrado");
    }

    @Test
    void obtenerConVarianteInexistenteFalla() {
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(
            CodigoCertificado.codificar(ID_VARIANTE)))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Certificado no encontrado");
    }

    @Test
    void obtenerSinLedgerFalla() {
        ProductoVariante variante = varianteConProducto();
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        VarianteLedgerDTO sinLedger =
            VarianteLedgerDTO.builder().tieneLedger(false).build();
        when(ledgerService.verificarVariante(variante))
            .thenReturn(sinLedger);

        assertThatThrownBy(() -> service.obtener(
            CodigoCertificado.codificar(ID_VARIANTE)))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("pieza no tiene certificado");
    }

    @Test
    void obtenerOpcionalDevuelveElCertificadoOUnVacio() {
        ProductoVariante variante = varianteConProducto();
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        when(ledgerService.verificarVariante(variante))
            .thenReturn(ledger(List.of()));

        assertThat(service.obtenerOpcional(
            CodigoCertificado.codificar(ID_VARIANTE))).isPresent();
        assertThat(service.obtenerOpcional("NO-EXISTE")).isEmpty();
    }

    @Test
    void pruebaOtsPrefiereLaPruebaConfirmada() {
        ProductoVariante variante = varianteConProducto();
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        when(ledgerService.verificarVariante(variante))
            .thenReturn(ledger(List.of(
                evento(100L, 0L, TipoEventoLedger.GENESIS))));
        AnclajeLedger anclaje = anclaje(
            200L, EstadoAnclaje.CONFIRMADO, null, null);
        anclaje.setPruebaOts("sin-confirmar".getBytes(StandardCharsets.UTF_8));
        anclaje.setPruebaConfirmada("confirmada".getBytes(StandardCharsets.UTF_8));
        when(anclajeService.mejorAnclajeDeEventos(anyCollection()))
            .thenReturn(Optional.of(anclaje));

        assertThat(service.pruebaOts(
            CodigoCertificado.codificar(ID_VARIANTE)))
            .hasValueSatisfying(bytes -> assertThat(bytes)
                .containsExactly("confirmada".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void pruebaOtsUsaLaPruebaPendienteSiNoHayConfirmada() {
        ProductoVariante variante = varianteConProducto();
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        when(ledgerService.verificarVariante(variante))
            .thenReturn(ledger(List.of(
                evento(100L, 0L, TipoEventoLedger.GENESIS))));
        AnclajeLedger anclaje = anclaje(
            300L, EstadoAnclaje.PENDIENTE, null, null);
        anclaje.setPruebaOts("pendiente".getBytes(StandardCharsets.UTF_8));
        when(anclajeService.mejorAnclajeDeEventos(anyCollection()))
            .thenReturn(Optional.of(anclaje));

        assertThat(service.pruebaOts(
            CodigoCertificado.codificar(ID_VARIANTE)))
            .hasValueSatisfying(bytes -> assertThat(bytes)
                .containsExactly("pendiente".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void pruebaOtsYDocumentoDevuelvenVacioSiNoHayCertificado() {
        assertThat(service.pruebaOts("NO-EXISTE")).isEmpty();
        assertThat(service.documento("NO-EXISTE")).isEmpty();

        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.empty());
        assertThat(service.pruebaOts(
            CodigoCertificado.codificar(ID_VARIANTE))).isEmpty();
        assertThat(service.documento(
            CodigoCertificado.codificar(ID_VARIANTE))).isEmpty();
    }

    @Test
    void pruebaOtsYDocumentoDevuelvenVacioSinLedgerOAnclaje() {
        ProductoVariante variante = varianteConProducto();
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        when(ledgerService.verificarVariante(variante))
            .thenReturn(VarianteLedgerDTO.builder()
                .tieneLedger(false).build());

        assertThat(service.pruebaOts(
            CodigoCertificado.codificar(ID_VARIANTE))).isEmpty();

        when(ledgerService.verificarVariante(variante))
            .thenReturn(ledger(List.of(
                evento(100L, 0L, TipoEventoLedger.GENESIS))));
        when(anclajeService.mejorAnclajeDeEventos(anyCollection()))
            .thenReturn(Optional.empty());

        assertThat(service.documento(
            CodigoCertificado.codificar(ID_VARIANTE))).isEmpty();
    }

    @Test
    void documentoDevuelveLaRaizDeMerkleEnTexto() {
        ProductoVariante variante = varianteConProducto();
        when(varianteRepository.findById(ID_VARIANTE))
            .thenReturn(Optional.of(variante));
        when(ledgerService.verificarVariante(variante))
            .thenReturn(ledger(List.of(
                evento(100L, 0L, TipoEventoLedger.GENESIS))));
        AnclajeLedger anclaje = anclaje(
            200L, EstadoAnclaje.CONFIRMADO, null, null);
        anclaje.setPruebaOts("ots".getBytes(StandardCharsets.UTF_8));
        when(anclajeService.mejorAnclajeDeEventos(anyCollection()))
            .thenReturn(Optional.of(anclaje));

        assertThat(service.documento(
            CodigoCertificado.codificar(ID_VARIANTE)))
            .hasValueSatisfying(bytes -> assertThat(bytes)
                .containsExactly("raiz-200".getBytes(StandardCharsets.UTF_8)));
    }

    private static ProductoVariante varianteConProducto() {
        Usuario artista = new Usuario();
        artista.setIdUsuario(5L);
        artista.setNombreUsuario("artista");
        artista.setNombreDisplay("La Artista");
        artista.setUrlFotoPerfilMiniatura("miniatura.png");
        artista.setUrlFotoPerfil("perfil.png");

        Producto producto = new Producto();
        producto.setIdProducto(10L);
        producto.setTitulo("Pieza");
        producto.setArtista(artista);
        producto.setImagenPortada("portada.png");

        Usuario propietario = new Usuario();
        propietario.setNombreUsuario("duenio");
        propietario.setMostrarPropietario(true);

        ProductoVariante variante = new ProductoVariante();
        variante.setIdVariante(ID_VARIANTE);
        variante.setNombre("Edición 1/1");
        variante.setProducto(producto);
        variante.setPropietarioActual(propietario);
        return variante;
    }

    private static VarianteLedgerDTO ledger(List<EventoLedgerDTO> eventos) {
        return VarianteLedgerDTO.builder()
            .idVariante(ID_VARIANTE)
            .tieneLedger(true)
            .integridad(true)
            .mensaje("Cadena válida")
            .eventos(eventos)
            .build();
    }

    private static EventoLedgerDTO evento(
            Long id, Long secuencia, TipoEventoLedger tipo) {
        return EventoLedgerDTO.builder()
            .idEventoLedger(id)
            .secuencia(secuencia)
            .tipo(tipo)
            .ocurridoEn(LocalDateTime.of(2026, 11, 18, 10, 0))
            .verificado(true)
            .build();
    }

    private static AnclajeLedger anclaje(
            Long id, EstadoAnclaje estado, LocalDateTime confirmacion,
            LocalDateTime solicitud) {
        AnclajeLedger anclaje = new AnclajeLedger();
        anclaje.setIdAnclaje(id);
        anclaje.setEstado(estado);
        anclaje.setHashRaiz("raiz-" + id);
        anclaje.setHashBitcoin(estado == EstadoAnclaje.CONFIRMADO
            ? "btc-" + id : null);
        anclaje.setBitcoinAltura(estado == EstadoAnclaje.CONFIRMADO
            ? id : null);
        anclaje.setFechaConfirmacion(confirmacion);
        anclaje.setFechaSolicitud(solicitud);
        return anclaje;
    }
}
