package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoDTO;
import com.temipora.backend_springboot.modelo.ProductoImagen;
import com.temipora.backend_springboot.modelo.ProductoImagenRequest;
import com.temipora.backend_springboot.modelo.ProductoRequest;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.ProductoVarianteRequest;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ProductoImagenRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;
import com.temipora.backend_springboot.repositorios.TipoProductoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    private static final String SHA_IMAGEN = "a".repeat(64);
    private static final String SHA_VARIANTE = "b".repeat(64);

    @Mock private ProductoRepository productoRepository;
    @Mock private ProductoVarianteRepository varianteRepository;
    @Mock private ProductoImagenRepository imagenRepository;
    @Mock private EventoLedgerRepository eventoLedgerRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TipoProductoRepository tipoProductoRepository;
    @Mock private MedioRepository medioRepository;
    @Mock private EtiquetaRepository etiquetaRepository;
    @Mock private MonedaService monedaService;
    @Mock private TerminosService terminosService;
    @Mock private LedgerService ledgerService;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks private ProductoService productoService;

    @Test
    void crear_ok_guardaVariantesYCreaGenesisParaLaUnica() {
        prepararCreacion();

        ProductoDTO dto = productoService.crear(5L, request());

        assertThat(dto.getIdProducto()).isEqualTo(10L);
        assertThat(dto.getTitulo()).isEqualTo("Plancha de otoño");
        assertThat(dto.getVariantes()).hasSize(1);
        assertThat(dto.getVariantes().get(0).isTieneLedger()).isTrue();
        assertThat(dto.getPrecioDesde()).isEqualByComparingTo("100.00");
        assertThat(dto.getContenidosSensibles()).isEmpty();
        verify(ledgerService).registrarGenesis(any(ProductoVariante.class), eq("hashTerminos"));
        verify(auditoriaService).registrar(
            eq(AccionesAdmin.PRODUCTO_CREAR), eq("Producto"), eq(10L),
            anyString(), isNull(), any(Producto.class), anyList());
    }

    @Test
    void crear_tituloRepetido_lanzaErrorYNoGuarda() {
        when(productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndEstadoNot(
                5L, 1L, "Plancha de otoño", EstadoProducto.ARCHIVADO))
            .thenReturn(true);

        assertThatThrownBy(() -> productoService.crear(5L, request()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ya tenés un producto con ese título y tipo.");

        verify(productoRepository, never()).saveAndFlush(any(Producto.class));
    }

    @Test
    void crear_sinTerminosGlobales_lanzaError() {
        when(productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndEstadoNot(
                5L, 1L, "Plancha de otoño", EstadoProducto.ARCHIVADO))
            .thenReturn(false);
        when(terminosService.tieneGlobalValido(5L)).thenReturn(false);

        assertThatThrownBy(() -> productoService.crear(5L, request()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Completá tus términos de uso antes de publicar el producto.");
    }

    @Test
    void crear_shaDuplicado_lanzaConflicto() {
        when(productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndEstadoNot(
                5L, 1L, "Plancha de otoño", EstadoProducto.ARCHIVADO))
            .thenReturn(false);
        when(terminosService.tieneGlobalValido(5L)).thenReturn(true);
        when(varianteRepository
            .existsBySha256AndEstadoNotAndProductoIdProductoNot(
                anyString(), eq(EstadoVariante.ARCHIVADO), eq(-1L)))
            .thenReturn(true);

        assertThatThrownBy(() -> productoService.crear(5L, request()))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("Ya existe un producto con la misma imagen");
    }

    @Test
    void crear_reventaSinVarianteUnica_lanzaError() {
        when(productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndEstadoNot(
                5L, 1L, "Plancha de otoño", EstadoProducto.ARCHIVADO))
            .thenReturn(false);
        when(terminosService.tieneGlobalValido(5L)).thenReturn(true);
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));
        when(tipoProductoRepository.findByIdTipoProducto(1L))
            .thenReturn(Optional.of(tipo(1L, "Adoptable")));
        when(medioRepository.findByIdMedio(2L))
            .thenReturn(Optional.of(medio(2L, "Digital")));
        when(monedaService.obtener(3L)).thenReturn(moneda(3L, "ARS"));

        ProductoRequest request = request();
        request.setPermiteReventa(true);
        request.getVariantes().get(0).setStockDisponible(2);

        assertThatThrownBy(() -> productoService.crear(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La reventa solo se habilita si hay al menos una variante con stock 1.");
    }

    @Test
    void crear_sinVariantes_lanzaError() {
        when(productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndEstadoNot(
                5L, 1L, "Plancha de otoño", EstadoProducto.ARCHIVADO))
            .thenReturn(false);
        when(terminosService.tieneGlobalValido(5L)).thenReturn(true);

        ProductoRequest request = request();
        request.setVariantes(new ArrayList<>());

        assertThatThrownBy(() -> productoService.crear(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Debés agregar al menos una variante.");
    }

    @Test
    void modificar_cambiaTituloYMantieneLaVarianteUnica() {
        Producto producto = productoConVariante(10L, 5L, 20L, SHA_VARIANTE);
        prepararModificacion(producto);

        ProductoRequest request = request();
        request.setTitulo("Plancha nueva");
        request.getVariantes().get(0).setIdVariante(20L);
        request.getVariantes().get(0).setSha256(SHA_VARIANTE);
        request.getVariantes().get(0).setClaveArchivoOriginal(null);

        ProductoDTO dto = productoService.modificar(10L, 5L, request);

        assertThat(dto.getTitulo()).isEqualTo("Plancha nueva");
        assertThat(dto.getVariantes().get(0).getIdVariante()).isEqualTo(20L);
        verify(ledgerService, never()).registrarGenesis(any(), anyString());
    }

    @Test
    void modificar_varianteUnicaConGenesis_noPermiteCambiarImagen() {
        Producto producto = productoConVariante(10L, 5L, 20L, SHA_VARIANTE);
        when(productoRepository.findByIdProductoForUpdate(10L))
            .thenReturn(Optional.of(producto));
        when(productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndIdProductoNotAndEstadoNot(
                5L, 1L, "Plancha nueva", 10L, EstadoProducto.ARCHIVADO))
            .thenReturn(false);
        when(terminosService.tieneProductoValido(10L, 5L)).thenReturn(true);
        when(tipoProductoRepository.findByIdTipoProducto(1L))
            .thenReturn(Optional.of(tipo(1L, "Adoptable")));
        when(medioRepository.findByIdMedio(2L))
            .thenReturn(Optional.of(medio(2L, "Digital")));
        when(monedaService.obtener(3L)).thenReturn(moneda(3L, "ARS"));
        when(eventoLedgerRepository.existsByVarianteIdVariante(20L)).thenReturn(true);

        ProductoRequest request = request();
        ProductoVarianteRequest variante = request.getVariantes().get(0);
        variante.setIdVariante(20L);
        variante.setSha256("c".repeat(64));
        variante.setUrlImagen("otra.webp");
        variante.setClaveArchivoOriginal("productos/otra.webp");
        request.setTitulo("Plancha nueva");

        assertThatThrownBy(() -> productoService.modificar(10L, 5L, request))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("no se puede cambiar");
    }

    @Test
    void modificar_noDuenio_lanzaError() {
        Producto producto = productoConVariante(10L, 99L, 20L, SHA_VARIANTE);
        when(productoRepository.findByIdProductoForUpdate(10L))
            .thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> productoService.modificar(10L, 5L, request()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No podés modificar un producto que no es tuyo.");
    }

    @Test
    void pausar_cambiaElEstadoYPersiste() {
        Producto producto = productoConVariante(10L, 5L, 20L, SHA_VARIANTE);
        when(productoRepository.findByIdProductoForUpdate(10L))
            .thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.findByIdProducto(10L))
            .thenReturn(Optional.of(producto));
        when(eventoLedgerRepository.findVariantesConLedger(List.of(10L)))
            .thenReturn(Set.of(20L));

        ProductoDTO dto = productoService.pausar(10L, 5L);

        assertThat(dto.getEstado()).isEqualTo(EstadoProducto.PAUSADO);
        verify(auditoriaService).registrar(
            eq(AccionesAdmin.PRODUCTO_PAUSAR), eq("Producto"), eq(10L),
            anyString(), any(Snapshot.class), any(Producto.class), anyList());
    }

    @Test
    void reactivar_sinTerminos_lanzaError() {
        Producto producto = productoConVariante(10L, 5L, 20L, SHA_VARIANTE);
        producto.setEstado(EstadoProducto.PAUSADO);
        when(productoRepository.findByIdProductoForUpdate(10L))
            .thenReturn(Optional.of(producto));
        when(terminosService.tieneProductoValido(10L, 5L)).thenReturn(false);

        assertThatThrownBy(() -> productoService.reactivar(10L, 5L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Completá tus términos de uso antes de publicar el producto.");
    }

    @Test
    void reactivar_pasaDePausadoADisponible() {
        Producto producto = productoConVariante(10L, 5L, 20L, SHA_VARIANTE);
        producto.setEstado(EstadoProducto.PAUSADO);
        when(productoRepository.findByIdProductoForUpdate(10L))
            .thenReturn(Optional.of(producto));
        when(terminosService.tieneProductoValido(10L, 5L)).thenReturn(true);
        when(productoRepository.save(any(Producto.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.findByIdProducto(10L))
            .thenReturn(Optional.of(producto));
        when(eventoLedgerRepository.findVariantesConLedger(List.of(10L)))
            .thenReturn(Set.of(20L));

        ProductoDTO dto = productoService.reactivar(10L, 5L);

        assertThat(dto.getEstado()).isEqualTo(EstadoProducto.DISPONIBLE);
    }

    @Test
    void listarMios_marcaVariantesConLedger() {
        Producto producto = productoConVariante(10L, 5L, 20L, SHA_VARIANTE);
        when(productoRepository
            .findByArtistaIdUsuarioAndEstadoNotOrderByTituloAsc(
                5L, EstadoProducto.ARCHIVADO))
            .thenReturn(List.of(producto));
        when(eventoLedgerRepository.findVariantesConLedger(List.of(10L)))
            .thenReturn(Set.of(20L));

        List<ProductoDTO> dtos = productoService.listarMios(5L);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getVariantes().get(0).isTieneLedger()).isTrue();
    }

    @Test
    void obtenerPropio_noDuenio_lanzaError() {
        Producto producto = productoConVariante(10L, 99L, 20L, SHA_VARIANTE);
        when(productoRepository.findByIdProducto(10L))
            .thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> productoService.obtenerPropio(10L, 5L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No podés modificar un producto que no es tuyo.");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void prepararCreacion() {
        when(productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndEstadoNot(
                5L, 1L, "Plancha de otoño", EstadoProducto.ARCHIVADO))
            .thenReturn(false);
        when(terminosService.tieneGlobalValido(5L)).thenReturn(true);
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));
        when(tipoProductoRepository.findByIdTipoProducto(1L))
            .thenReturn(Optional.of(tipo(1L, "Adoptable")));
        when(medioRepository.findByIdMedio(2L))
            .thenReturn(Optional.of(medio(2L, "Digital")));
        when(monedaService.obtener(3L)).thenReturn(moneda(3L, "ARS"));
        when(productoRepository.saveAndFlush(any(Producto.class)))
            .thenAnswer(inv -> {
                Producto producto = inv.getArgument(0);
                producto.setIdProducto(10L);
                long id = 20L;
                for (ProductoVariante variante : producto.getVariantes()) {
                    variante.setIdVariante(id++);
                }
                return producto;
            });
        when(terminosService.hashVigenteProducto(10L, 5L)).thenReturn("hashTerminos");
        when(eventoLedgerRepository.findVariantesConLedger(List.of(10L)))
            .thenReturn(Set.of(20L));
    }

    private void prepararModificacion(Producto producto) {
        when(productoRepository.findByIdProductoForUpdate(10L))
            .thenReturn(Optional.of(producto));
        when(productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndIdProductoNotAndEstadoNot(
                5L, 1L, "Plancha nueva", 10L, EstadoProducto.ARCHIVADO))
            .thenReturn(false);
        when(terminosService.tieneProductoValido(10L, 5L)).thenReturn(true);
        when(tipoProductoRepository.findByIdTipoProducto(1L))
            .thenReturn(Optional.of(tipo(1L, "Adoptable")));
        when(medioRepository.findByIdMedio(2L))
            .thenReturn(Optional.of(medio(2L, "Digital")));
        when(monedaService.obtener(3L)).thenReturn(moneda(3L, "ARS"));
        when(productoRepository.saveAndFlush(any(Producto.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(terminosService.hashVigenteProducto(10L, 5L)).thenReturn("hashTerminos");
        when(eventoLedgerRepository.existsByVarianteIdVariante(20L)).thenReturn(true);
        when(eventoLedgerRepository.findVariantesConLedger(List.of(10L)))
            .thenReturn(Set.of(20L));
    }

    private ProductoRequest request() {
        ProductoRequest request = new ProductoRequest();
        request.setIdTipoProducto(1L);
        request.setIdMedio(2L);
        request.setIdMoneda(3L);
        request.setTitulo("Plancha de otoño");
        request.setDescripcion("desc");
        request.setContenidosSensibles(Set.of());
        request.setPorcentajeRegalia(new BigDecimal("5"));
        request.setImagenes(new ArrayList<>(List.of(imagenRequest())));
        request.setVariantes(new ArrayList<>(List.of(varianteRequest(null))));
        request.setIdsEtiquetas(Set.of());
        return request;
    }

    private ProductoImagenRequest imagenRequest() {
        ProductoImagenRequest imagen = new ProductoImagenRequest();
        imagen.setUrl("img.webp");
        imagen.setSha256(SHA_IMAGEN);
        imagen.setClaveArchivoOriginal("productos/img.webp");
        imagen.setOrden(0);
        return imagen;
    }

    private ProductoVarianteRequest varianteRequest(Long idVariante) {
        ProductoVarianteRequest variante = new ProductoVarianteRequest();
        variante.setIdVariante(idVariante);
        variante.setNombre("Única");
        variante.setDescripcion("");
        variante.setPrecio(new BigDecimal("100.00"));
        variante.setStockDisponible(1);
        variante.setOrden(0);
        variante.setEstado(EstadoVariante.DISPONIBLE);
        variante.setUrlImagen("var.webp");
        variante.setSha256(SHA_VARIANTE);
        variante.setClaveArchivoOriginal("productos/var.webp");
        return variante;
    }

    private Producto productoConVariante(
            Long idProducto, Long idArtista, Long idVariante, String sha) {
        Producto producto = new Producto();
        producto.setIdProducto(idProducto);
        producto.setArtista(usuario(idArtista));
        producto.setTipoProducto(tipo(1L, "Adoptable"));
        producto.setMedio(medio(2L, "Digital"));
        producto.setMoneda(moneda(3L, "ARS"));
        producto.setTitulo("Plancha de otoño");
        producto.setDescripcion("desc");
        producto.setEstado(EstadoProducto.DISPONIBLE);
        producto.setImagenPortada("img.webp");
        producto.setContenidosSensibles(new HashSet<>());

        ProductoVariante variante = new ProductoVariante();
        variante.setIdVariante(idVariante);
        variante.setProducto(producto);
        variante.setNombre("Única");
        variante.setPrecio(new BigDecimal("100.00"));
        variante.setStockDisponible(1);
        variante.setOrden(0);
        variante.setEstado(EstadoVariante.DISPONIBLE);
        variante.setImagenUrl("var.webp");
        variante.setSha256(sha);
        variante.setClaveArchivoOriginal("productos/var.webp");
        variante.setPropietarioActual(usuario(idArtista));
        producto.getVariantes().add(variante);

        ProductoImagen imagen = new ProductoImagen();
        imagen.setProducto(producto);
        imagen.setUrl("img.webp");
        imagen.setOrden(0);
        imagen.setSha256(SHA_IMAGEN);
        producto.getImagenes().add(imagen);

        return producto;
    }

    private Moneda moneda(Long id, String codigo) {
        return Moneda.builder().idMoneda(id).codigo(codigo).nombre(codigo).build();
    }

    private TipoProducto tipo(Long id, String nombre) {
        return TipoProducto.builder()
            .idTipoProducto(id)
            .nombre(nombre)
            .descripcion("desc")
            .activo(true)
            .build();
    }

    private Medio medio(Long id, String nombre) {
        return Medio.builder().idMedio(id).nombre(nombre).activo(true).build();
    }

    private Usuario usuario(Long id) {
        return Usuario.builder().idUsuario(id).nombreUsuario("artista").build();
    }
}
