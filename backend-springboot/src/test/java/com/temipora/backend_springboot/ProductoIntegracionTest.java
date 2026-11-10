package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.EventoLedger;
import com.temipora.backend_springboot.modelo.LicenciaProducto;
import com.temipora.backend_springboot.modelo.LicenciaProductoRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoDTO;
import com.temipora.backend_springboot.modelo.ProductoImagenRequest;
import com.temipora.backend_springboot.modelo.ProductoLedgerDTO;
import com.temipora.backend_springboot.modelo.ProductoRequest;
import com.temipora.backend_springboot.modelo.ProductoVarianteDTO;
import com.temipora.backend_springboot.modelo.ProductoVarianteRequest;
import com.temipora.backend_springboot.modelo.TipoEventoLedger;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.TipoProductoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.LedgerService;
import com.temipora.backend_springboot.servicios.MonedaService;
import com.temipora.backend_springboot.servicios.ProductoService;

@SpringBootTest
@Transactional
class ProductoIntegracionTest {

    @Autowired private ProductoService productoService;
    @Autowired private LedgerService ledgerService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoProductoRepository tipoProductoRepository;
    @Autowired private MedioRepository medioRepository;
    @Autowired private MonedaService monedaService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private EventoLedgerRepository eventoLedgerRepository;
    @Autowired private DerechoTerminoRepository derechoTerminoRepository;

    @Test
    void crearProductoUnico_persisteVariantesLicenciaYGenesis() {
        Usuario artista = usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getRol() != null
                && usuario.getRol().getNombre() == ERol.ARTISTA)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No hay un usuario ARTISTA para la prueba de integración."));
        TipoProducto tipo = tipoProductoRepository
            .findByActivoTrueOrderByNombreAsc().get(0);
        Medio medio = medioRepository.findByActivoTrueOrderByNombreAsc().get(0);
        Moneda moneda = monedaService.predeterminada();

        String titulo = "Test productos " + UUID.randomUUID();
        String shaVariante = shaAleatorio();

        ProductoVarianteRequest variante = new ProductoVarianteRequest();
        variante.setNombre("Única");
        variante.setPrecio(new BigDecimal("150.00"));
        variante.setStockDisponible(1);
        variante.setOrden(0);
        variante.setEstado(EstadoVariante.DISPONIBLE);
        variante.setUrlImagen("/uploads/test-variante.webp");
        variante.setSha256(shaVariante);
        variante.setClaveArchivoOriginal("productos/test-variante.webp");

        List<ProductoImagenRequest> imagenes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ProductoImagenRequest imagen = new ProductoImagenRequest();
            imagen.setUrl("/uploads/test-producto-" + i + ".webp");
            imagen.setSha256(shaAleatorio());
            imagen.setClaveArchivoOriginal("productos/test-producto-" + i + ".webp");
            imagen.setOrden(i);
            imagenes.add(imagen);
        }

        ProductoRequest request = new ProductoRequest();
        request.setIdTipoProducto(tipo.getIdTipoProducto());
        request.setIdMedio(medio.getIdMedio());
        request.setIdMoneda(moneda.getIdMoneda());
        request.setTitulo(titulo);
        request.setDescripcion("Producto de prueba de integración.");
        request.setImagenes(imagenes);
        request.setVariantes(new ArrayList<>(List.of(variante)));
        request.setIdsEtiquetas(Set.of());
        request.setContenidosSensibles(Set.of());

        List<DerechoTermino> derechosActivos = derechoTerminoRepository
            .findByActivoTrueOrderByOrdenAsc();
        DerechoTermino personal = derechosActivos.stream()
            .filter(derecho -> "PERSONAL".equals(derecho.getCodigo()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No existe el derecho PERSONAL en el catálogo."));
        DerechoTermino contenido = derechosActivos.stream()
            .filter(derecho -> "CONTENIDO".equals(derecho.getCodigo()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No existe el derecho CONTENIDO en el catálogo."));
        LicenciaProductoRequest licencia = new LicenciaProductoRequest();
        licencia.setIdsDerechos(Set.of(
            personal.getIdDerechoTermino(),
            contenido.getIdDerechoTermino()));
        licencia.setPermiteModificaciones(false);
        licencia.setRequiereCredito(true);
        licencia.setPorcentajeRegalia(BigDecimal.ZERO);
        licencia.setTextoAdicional("Uso personal de la pieza.");
        request.setLicencia(licencia);

        ProductoDTO dto = productoService.crear(artista.getIdUsuario(), request);

        assertThat(dto.getIdProducto()).isNotNull();
        assertThat(dto.getTitulo()).isEqualTo(titulo);
        assertThat(dto.getVariantes()).hasSize(1);
        assertThat(dto.getVariantes().get(0).isTieneLedger()).isTrue();
        assertThat(dto.getImagenes()).hasSize(4);
        assertThat(dto.getPrecioDesde()).isEqualByComparingTo("150.00");
        assertThat(dto.getTipoEntregaMedio()).isEqualTo(TipoEntrega.DIGITAL);

        assertThat(dto.getLicencia()).isNotNull();
        assertThat(dto.getLicencia().getDerechos())
            .extracting(derecho -> derecho.getCodigo())
            .containsExactlyInAnyOrder("PERSONAL", "CONTENIDO");

        Producto producto = productoRepository.findByIdProducto(dto.getIdProducto())
            .orElseThrow();
        assertThat(producto.getVariantes()).hasSize(1);
        assertThat(producto.getImagenes()).hasSize(4);
        assertThat(producto.getLicencias()).hasSize(1);
        LicenciaProducto licenciaGuardada = producto.licenciaVigente();
        assertThat(licenciaGuardada).isNotNull();
        assertThat(licenciaGuardada.getDerechos()).hasSize(2);
        assertThat(licenciaGuardada.isRequiereCredito()).isTrue();
        Long idVariante = producto.getVariantes().iterator().next().getIdVariante();

        PaginaDTO<ProductoVarianteDTO> paginaVariantes = productoService
            .obtenerVariantes(dto.getIdProducto(), artista.getIdUsuario(), 1, 4);
        assertThat(paginaVariantes.getContenido()).hasSize(1);
        assertThat(paginaVariantes.getTotalElementos()).isEqualTo(1);
        assertThat(paginaVariantes.getTotalPaginas()).isEqualTo(1);

        ProductoLedgerDTO ledgerPagina = ledgerService
            .verificarProducto(producto, 1, 4);
        assertThat(ledgerPagina.getVariantes()).hasSize(1);
        assertThat(ledgerPagina.getTotalElementos()).isEqualTo(1);

        List<EventoLedger> eventos = eventoLedgerRepository
            .findByVarianteIdVarianteOrderBySecuenciaAsc(idVariante);
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).getTipo()).isEqualTo(TipoEventoLedger.GENESIS);
        assertThat(eventos.get(0).getHashEvento()).hasSize(64);
        assertThat(eventos.get(0).getHashImagen()).isEqualTo(shaVariante);

        assertThat(ledgerService.verificarProducto(producto).isIntegridad()).isTrue();
    }

    @Test
    void guardarBorrador_sinPrecioNiImagenes_persisteComoBorrador() {
        Usuario artista = usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getRol() != null
                && usuario.getRol().getNombre() == ERol.ARTISTA)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No hay un usuario ARTISTA para la prueba de integración."));

        ProductoVarianteRequest variante = new ProductoVarianteRequest();
        variante.setNombre("Única");
        variante.setPrecio(null);
        variante.setStockDisponible(null);
        variante.setOrden(0);
        variante.setEstado(EstadoVariante.DISPONIBLE);
        variante.setSha256("");

        ProductoRequest request = new ProductoRequest();
        request.setTitulo("Borrador de integración " + UUID.randomUUID());
        request.setVariantes(new ArrayList<>(List.of(variante)));
        request.setImagenes(new ArrayList<>());
        request.setIdsEtiquetas(Set.of());
        request.setContenidosSensibles(Set.of());

        ProductoDTO dto = productoService.guardarBorrador(
            artista.getIdUsuario(), null, request);

        assertThat(dto.getIdProducto()).isNotNull();
        assertThat(dto.getEstado()).isEqualTo(EstadoProducto.BORRADOR);
        assertThat(dto.getVariantes()).hasSize(1);
        assertThat(dto.getVariantes().get(0).getPrecio()).isNull();
        assertThat(dto.getVariantes().get(0).isTieneLedger()).isFalse();
        assertThat(dto.getImagenes()).isEmpty();
        assertThat(dto.getPrecioDesde()).isNull();
    }

    @Test
    void guardarBorrador_dosVeces_noDuplicaLicenciaNiVariantes() {
        Usuario artista = usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getRol() != null
                && usuario.getRol().getNombre() == ERol.ARTISTA)
            .findFirst()
            .orElseThrow();

        ProductoVarianteRequest variante = new ProductoVarianteRequest();
        variante.setNombre("Única");
        variante.setOrden(0);

        ProductoRequest request = new ProductoRequest();
        request.setTitulo("Borrador repetido " + UUID.randomUUID());
        request.setVariantes(new ArrayList<>(List.of(variante)));
        request.setImagenes(new ArrayList<>());
        request.setIdsEtiquetas(Set.of());
        request.setContenidosSensibles(Set.of());

        ProductoDTO primero = productoService.guardarBorrador(
            artista.getIdUsuario(), null, request);
        Long idVariante = primero.getVariantes().get(0).getIdVariante();

        ProductoRequest segundo = new ProductoRequest();
        segundo.setTitulo(primero.getTitulo());
        ProductoVarianteRequest misma = new ProductoVarianteRequest();
        misma.setIdVariante(idVariante);
        misma.setNombre("Única");
        misma.setOrden(0);
        segundo.setVariantes(new ArrayList<>(List.of(misma)));
        segundo.setImagenes(new ArrayList<>());
        segundo.setIdsEtiquetas(Set.of());
        segundo.setContenidosSensibles(Set.of());

        ProductoDTO actualizado = productoService.guardarBorrador(
            artista.getIdUsuario(), primero.getIdProducto(), segundo);

        assertThat(actualizado.getEstado()).isEqualTo(EstadoProducto.BORRADOR);
        assertThat(actualizado.getVariantes()).hasSize(1);
    }

    private String shaAleatorio() {
        return (UUID.randomUUID().toString() + UUID.randomUUID().toString())
            .replace("-", "");
    }
}
