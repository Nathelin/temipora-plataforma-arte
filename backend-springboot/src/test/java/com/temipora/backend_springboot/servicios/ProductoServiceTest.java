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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.LicenciaProducto;
import com.temipora.backend_springboot.modelo.LicenciaProductoRequest;
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
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;
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
    @Mock private DerechoTerminoRepository derechoTerminoRepository;
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
        assertThat(dto.getLicencia()).isNotNull();
        assertThat(dto.getLicencia().getDerechos()).hasSize(1);
        assertThat(dto.getLicencia().getDerechos().get(0).getCodigo())
            .isEqualTo("CONTENIDO");
        verify(ledgerService).registrarGenesis(
            any(ProductoVariante.class), anyString());
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
    void crear_sinDerechos_lanzaError() {
        when(productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndEstadoNot(
                5L, 1L, "Plancha de otoño", EstadoProducto.ARCHIVADO))
            .thenReturn(false);
        ProductoRequest request = request();
        request.getLicencia().setIdsDerechos(Set.of());

        assertThatThrownBy(() -> productoService.crear(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Elegí al menos un uso permitido para la licencia.");
    }

    @Test
    void crear_shaDuplicado_lanzaConflicto() {
        when(productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndEstadoNot(
                5L, 1L, "Plancha de otoño", EstadoProducto.ARCHIVADO))
            .thenReturn(false);
        when(derechoTerminoRepository.findAllById(any()))
            .thenReturn(List.of(derecho(2L, "CONTENIDO", false)));
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
        when(derechoTerminoRepository.findAllById(any()))
            .thenReturn(List.of(derecho(1L, "REVENTA", true)));

        ProductoRequest request = request();
        request.getLicencia().setIdsDerechos(Set.of(1L));
        request.getVariantes().get(0).setStockDisponible(2);

        assertThatThrownBy(() -> productoService.crear(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("El derecho de reventa solo se habilita");
    }

    @Test
    void crear_sinVariantes_lanzaError() {
        when(productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndEstadoNot(
                5L, 1L, "Plancha de otoño", EstadoProducto.ARCHIVADO))
            .thenReturn(false);
        when(derechoTerminoRepository.findAllById(any()))
            .thenReturn(List.of(derecho(2L, "CONTENIDO", false)));

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
        when(derechoTerminoRepository.findAllById(any()))
            .thenReturn(List.of(derecho(2L, "CONTENIDO", false)));
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
    void reactivar_sinLicencia_lanzaError() {
        Producto producto = productoConVariante(10L, 5L, 20L, SHA_VARIANTE);
        producto.getLicencias().clear();
        producto.setEstado(EstadoProducto.PAUSADO);
        when(productoRepository.findByIdProductoForUpdate(10L))
            .thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> productoService.reactivar(10L, 5L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El producto no tiene una licencia configurada.");
    }

    @Test
    void reactivar_pasaDePausadoADisponible() {
        Producto producto = productoConVariante(10L, 5L, 20L, SHA_VARIANTE);
        producto.setEstado(EstadoProducto.PAUSADO);
        when(productoRepository.findByIdProductoForUpdate(10L))
            .thenReturn(Optional.of(producto));
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
        assertThat(dtos.get(0).getLicencia()).isNotNull();
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

    @Test
    void crear_reventaConVarianteUnica_guardaRegalia() {
        prepararCreacion();
        when(derechoTerminoRepository.findAllById(any()))
            .thenReturn(List.of(derecho(1L, "REVENTA", true)));

        ProductoRequest request = request();
        request.getLicencia().setIdsDerechos(Set.of(1L));
        request.getLicencia().setPorcentajeRegalia(new BigDecimal("5.00"));

        ProductoDTO dto = productoService.crear(5L, request);

        assertThat(dto.getLicencia().isPermiteReventa()).isTrue();
        assertThat(dto.getLicencia().getPorcentajeRegalia())
            .isEqualByComparingTo("5.00");
        assertThat(dto.getPorcentajeRegalia()).isEqualByComparingTo("5.00");
        assertThat(dto.getLicencia().getDerechos().get(0).getCodigo())
            .isEqualTo("REVENTA");
    }

    @Test
    void modificar_cambiaLicencia_creaVersionNuevaYConservaLaAnterior() {
        Producto producto = productoConVariante(10L, 5L, 20L, SHA_VARIANTE);
        prepararModificacion(producto);
        when(derechoTerminoRepository.findAllById(any()))
            .thenReturn(List.of(derecho(1L, "REVENTA", true)));

        ProductoRequest request = request();
        request.setTitulo("Plancha nueva");
        request.getLicencia().setIdsDerechos(Set.of(1L));
        request.getLicencia().setPorcentajeRegalia(new BigDecimal("5.00"));
        request.getVariantes().get(0).setIdVariante(20L);
        request.getVariantes().get(0).setSha256(SHA_VARIANTE);
        request.getVariantes().get(0).setClaveArchivoOriginal(null);

        ProductoDTO dto = productoService.modificar(10L, 5L, request);

        assertThat(producto.getLicencias()).hasSize(2);
        assertThat(dto.getLicencia().getVersion()).isEqualTo(2);
        assertThat(dto.getLicencia().isPermiteReventa()).isTrue();
        assertThat(dto.getLicencia().getPorcentajeRegalia())
            .isEqualByComparingTo("5.00");
        assertThat(producto.getLicencias().stream()
            .filter(licencia -> !licencia.isVigente())
            .count()).isEqualTo(1);
    }

    @Test
    void modificar_varianteUsaImagenDeGaleriaSinReSubir() {
        Producto producto = productoConVariante(10L, 5L, 20L, SHA_VARIANTE);
        prepararModificacion(producto);
        when(eventoLedgerRepository.existsByVarianteIdVariante(20L)).thenReturn(false);

        ProductoRequest request = request();
        request.setTitulo("Plancha nueva");
        ProductoVarianteRequest variante = request.getVariantes().get(0);
        variante.setIdVariante(20L);
        variante.setUrlImagen("img.webp");
        variante.setSha256(SHA_IMAGEN);
        variante.setClaveArchivoOriginal(null);

        ProductoDTO dto = productoService.modificar(10L, 5L, request);

        assertThat(dto.getVariantes().get(0).getSha256()).isEqualTo(SHA_IMAGEN);
        assertThat(producto.getVariantes().get(0).getClaveArchivoOriginal())
            .isEqualTo("productos/img.webp");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void prepararCreacion() {
        when(productoRepository
            .existsByArtistaIdUsuarioAndTipoProductoIdTipoProductoAndTituloIgnoreCaseAndEstadoNot(
                5L, 1L, "Plancha de otoño", EstadoProducto.ARCHIVADO))
            .thenReturn(false);
        when(derechoTerminoRepository.findAllById(any()))
            .thenReturn(List.of(derecho(2L, "CONTENIDO", false)));
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
                long idLicencia = 30L;
                for (LicenciaProducto licencia : producto.getLicencias()) {
                    licencia.setIdLicenciaProducto(idLicencia++);
                }
                return producto;
            });
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
        when(derechoTerminoRepository.findAllById(any()))
            .thenReturn(List.of(derecho(2L, "CONTENIDO", false)));
        when(tipoProductoRepository.findByIdTipoProducto(1L))
            .thenReturn(Optional.of(tipo(1L, "Adoptable")));
        when(medioRepository.findByIdMedio(2L))
            .thenReturn(Optional.of(medio(2L, "Digital")));
        when(monedaService.obtener(3L)).thenReturn(moneda(3L, "ARS"));
        when(productoRepository.saveAndFlush(any(Producto.class)))
            .thenAnswer(inv -> inv.getArgument(0));
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
        request.setImagenes(new ArrayList<>(List.of(imagenRequest())));
        request.setVariantes(new ArrayList<>(List.of(varianteRequest(null))));
        request.setIdsEtiquetas(Set.of());
        request.setLicencia(licenciaRequest());
        return request;
    }

    private LicenciaProductoRequest licenciaRequest() {
        LicenciaProductoRequest licencia = new LicenciaProductoRequest();
        licencia.setIdsDerechos(new LinkedHashSet<>(Set.of(2L)));
        licencia.setPorcentajeRegalia(BigDecimal.ZERO);
        return licencia;
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
        imagen.setClaveArchivoOriginal("productos/img.webp");
        producto.getImagenes().add(imagen);

        LicenciaProducto licencia = new LicenciaProducto();
        licencia.setIdLicenciaProducto(30L);
        licencia.setProducto(producto);
        licencia.setVersion(1);
        licencia.setVigente(true);
        licencia.setPorcentajeRegalia(BigDecimal.ZERO);
        licencia.reemplazarDerechos(Set.of(derecho(2L, "CONTENIDO", false)));
        producto.getLicencias().add(licencia);

        return producto;
    }

    private DerechoTermino derecho(Long id, String codigo, boolean requierePiezaUnica) {
        DerechoTermino derecho = new DerechoTermino();
        derecho.setIdDerechoTermino(id);
        derecho.setCodigo(codigo);
        derecho.setNombre(codigo);
        derecho.setOrden(0);
        derecho.setActivo(true);
        derecho.setRequierePiezaUnica(requierePiezaUnica);
        return derecho;
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
