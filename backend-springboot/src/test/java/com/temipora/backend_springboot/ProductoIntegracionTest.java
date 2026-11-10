package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.temipora.backend_springboot.modelo.ProductoCatalogoDTO;
import com.temipora.backend_springboot.modelo.ProductoEntregableDTO;
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
import com.temipora.backend_springboot.repositorios.ProductoVarianteImagenRepository;
import com.temipora.backend_springboot.servicios.LedgerService;
import com.temipora.backend_springboot.servicios.MonedaService;
import com.temipora.backend_springboot.servicios.ProductoCatalogoService;
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
    @Autowired private ProductoVarianteImagenRepository varianteImagenRepository;
    @Autowired private ProductoCatalogoService productoCatalogoService;

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

    @Test
    void publicarProducto_cadaVarianteEntregaSuPropiaImagen() {
        Usuario artista = usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getRol() != null
                && usuario.getRol().getNombre() == ERol.ARTISTA)
            .findFirst()
            .orElseThrow();

        TipoProducto tipo = tipoProductoRepository
            .findByActivoTrueOrderByNombreAsc().get(0);
        Medio medio = medioRepository.findByActivoTrueOrderByNombreAsc().stream()
            .filter(m -> m.getTipoEntrega() == TipoEntrega.DIGITAL)
            .findFirst()
            .orElseThrow();
        Moneda moneda = monedaService.predeterminada();

        List<String> shas = List.of(shaAleatorio(), shaAleatorio());

        List<ProductoImagenRequest> imagenes = new ArrayList<>();
        for (int i = 0; i < shas.size(); i++) {
            ProductoImagenRequest imagen = new ProductoImagenRequest();
            imagen.setUrl("/uploads/dos-" + i + ".webp");
            imagen.setSha256(shas.get(i));
            imagen.setClaveArchivoOriginal("productos/dos-" + i + ".jpg");
            imagen.setOrden(i);
            imagenes.add(imagen);
        }

        List<ProductoVarianteRequest> variantes = new ArrayList<>();
        for (int i = 0; i < shas.size(); i++) {
            ProductoVarianteRequest variante = new ProductoVarianteRequest();
            variante.setNombre("Variante " + (i + 1));
            variante.setPrecio(new BigDecimal("100.00"));
            variante.setStockDisponible(null);
            variante.setOrden(i);
            variante.setUrlImagen(imagenes.get(i).getUrl());
            variante.setSha256(shas.get(i));
            variante.setClaveArchivoOriginal(imagenes.get(i).getClaveArchivoOriginal());
            variantes.add(variante);
        }

        ProductoRequest request = new ProductoRequest();
        request.setIdTipoProducto(tipo.getIdTipoProducto());
        request.setIdMedio(medio.getIdMedio());
        request.setIdMoneda(moneda.getIdMoneda());
        request.setTitulo("Dos variantes " + UUID.randomUUID());
        request.setDescripcion("Cada variante entrega su propia imagen.");
        request.setImagenes(imagenes);
        request.setVariantes(variantes);
        request.setIdsEtiquetas(Set.of());
        request.setContenidosSensibles(Set.of());

        DerechoTermino personal = derechoTerminoRepository
            .findByActivoTrueOrderByOrdenAsc().stream()
            .filter(derecho -> "PERSONAL".equals(derecho.getCodigo()))
            .findFirst()
            .orElseThrow();
        LicenciaProductoRequest licencia = new LicenciaProductoRequest();
        licencia.setIdsDerechos(Set.of(personal.getIdDerechoTermino()));
        licencia.setPorcentajeRegalia(BigDecimal.ZERO);
        request.setLicencia(licencia);

        ProductoDTO dto = productoService.crear(artista.getIdUsuario(), request);

        assertThat(dto.getEntregables()).hasSize(2);
        assertThat(dto.getEntregables())
            .extracting(ProductoEntregableDTO::getSha256)
            .containsExactlyInAnyOrderElementsOf(shas);
        // El entregable es la imagen de su propia variante.
        for (ProductoEntregableDTO entregable : dto.getEntregables()) {
            var variante = dto.getVariantes().stream()
                .filter(v -> v.getIdVariante().equals(entregable.getIdVariante()))
                .findFirst()
                .orElseThrow();
            assertThat(entregable.getSha256()).isEqualTo(variante.getSha256());
        }
    }

    @Test
    void publicarProducto_dosVariantesConLaMismaImagen_lanzaError() {
        Usuario artista = usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getRol() != null
                && usuario.getRol().getNombre() == ERol.ARTISTA)
            .findFirst()
            .orElseThrow();

        TipoProducto tipo = tipoProductoRepository
            .findByActivoTrueOrderByNombreAsc().get(0);
        Medio medio = medioRepository.findByActivoTrueOrderByNombreAsc().stream()
            .filter(m -> m.getTipoEntrega() == TipoEntrega.DIGITAL)
            .findFirst()
            .orElseThrow();
        Moneda moneda = monedaService.predeterminada();

        String shaCompartido = shaAleatorio();

        ProductoImagenRequest imagen = new ProductoImagenRequest();
        imagen.setUrl("/uploads/compartida.webp");
        imagen.setSha256(shaCompartido);
        imagen.setClaveArchivoOriginal("productos/compartida.jpg");
        imagen.setOrden(0);

        List<ProductoVarianteRequest> variantes = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ProductoVarianteRequest variante = new ProductoVarianteRequest();
            variante.setNombre("Variante " + (i + 1));
            variante.setPrecio(new BigDecimal("100.00"));
            variante.setStockDisponible(null);
            variante.setOrden(i);
            variante.setUrlImagen(imagen.getUrl());
            variante.setSha256(shaCompartido);
            variante.setClaveArchivoOriginal(imagen.getClaveArchivoOriginal());
            variantes.add(variante);
        }

        ProductoRequest request = new ProductoRequest();
        request.setIdTipoProducto(tipo.getIdTipoProducto());
        request.setIdMedio(medio.getIdMedio());
        request.setIdMoneda(moneda.getIdMoneda());
        request.setTitulo("Imagen compartida " + UUID.randomUUID());
        request.setDescripcion("Dos variantes con el mismo archivo.");
        request.setImagenes(new ArrayList<>(List.of(imagen)));
        request.setVariantes(variantes);
        request.setIdsEtiquetas(Set.of());
        request.setContenidosSensibles(Set.of());

        DerechoTermino personal = derechoTerminoRepository
            .findByActivoTrueOrderByOrdenAsc().stream()
            .filter(derecho -> "PERSONAL".equals(derecho.getCodigo()))
            .findFirst()
            .orElseThrow();
        LicenciaProductoRequest licencia = new LicenciaProductoRequest();
        licencia.setIdsDerechos(Set.of(personal.getIdDerechoTermino()));
        licencia.setPorcentajeRegalia(BigDecimal.ZERO);
        request.setLicencia(licencia);

        assertThatThrownBy(() -> productoService.crear(artista.getIdUsuario(), request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pueden entregar la misma imagen");
    }

    @Test
    void publicarProducto_sinSeleccionDeEntregables_usaLaImagenDeLaVariante() {
        Usuario artista = usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getRol() != null
                && usuario.getRol().getNombre() == ERol.ARTISTA)
            .findFirst()
            .orElseThrow();

        TipoProducto tipo = tipoProductoRepository
            .findByActivoTrueOrderByNombreAsc().get(0);
        Medio medio = medioRepository.findByActivoTrueOrderByNombreAsc().stream()
            .filter(m -> m.getTipoEntrega() == TipoEntrega.DIGITAL)
            .findFirst()
            .orElseThrow();
        Moneda moneda = monedaService.predeterminada();

        String shaPropio = shaAleatorio();
        ProductoImagenRequest imagen = new ProductoImagenRequest();
        imagen.setUrl("/uploads/solo.webp");
        imagen.setSha256(shaPropio);
        imagen.setClaveArchivoOriginal("productos/solo.jpg");
        imagen.setOrden(0);

        ProductoVarianteRequest variante = new ProductoVarianteRequest();
        variante.setNombre("Única");
        variante.setPrecio(new BigDecimal("100.00"));
        variante.setStockDisponible(null);
        variante.setOrden(0);
        variante.setUrlImagen("/uploads/solo.webp");
        variante.setSha256(shaPropio);
        variante.setClaveArchivoOriginal("productos/solo.jpg");
        // Sin lista explícita: se preselecciona la imagen de la variante.

        ProductoRequest request = new ProductoRequest();
        request.setIdTipoProducto(tipo.getIdTipoProducto());
        request.setIdMedio(medio.getIdMedio());
        request.setIdMoneda(moneda.getIdMoneda());
        request.setTitulo("Suelto " + UUID.randomUUID());
        request.setDescripcion("Producto de un solo archivo.");
        request.setImagenes(new ArrayList<>(List.of(imagen)));
        request.setVariantes(new ArrayList<>(List.of(variante)));
        request.setIdsEtiquetas(Set.of());
        request.setContenidosSensibles(Set.of());

        DerechoTermino personal = derechoTerminoRepository
            .findByActivoTrueOrderByOrdenAsc().stream()
            .filter(derecho -> "PERSONAL".equals(derecho.getCodigo()))
            .findFirst()
            .orElseThrow();
        LicenciaProductoRequest licencia = new LicenciaProductoRequest();
        licencia.setIdsDerechos(Set.of(personal.getIdDerechoTermino()));
        licencia.setPorcentajeRegalia(BigDecimal.ZERO);
        request.setLicencia(licencia);

        ProductoDTO dto = productoService.crear(artista.getIdUsuario(), request);

        List<ProductoEntregableDTO> entregables = dto.getEntregables();
        assertThat(entregables).hasSize(1);
        assertThat(entregables.get(0).getSha256()).isEqualTo(shaPropio);
    }

    @Test
    void modificarProducto_reemplazaEntregablesSinChocarConElIndiceUnico() {
        Usuario artista = usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getRol() != null
                && usuario.getRol().getNombre() == ERol.ARTISTA)
            .findFirst()
            .orElseThrow();

        TipoProducto tipo = tipoProductoRepository
            .findByActivoTrueOrderByNombreAsc().get(0);
        Medio medio = medioRepository.findByActivoTrueOrderByNombreAsc().stream()
            .filter(m -> m.getTipoEntrega() == TipoEntrega.DIGITAL)
            .findFirst()
            .orElseThrow();
        Moneda moneda = monedaService.predeterminada();

        List<String> shas = List.of(shaAleatorio(), shaAleatorio());

        List<ProductoImagenRequest> imagenes = new ArrayList<>();
        for (int i = 0; i < shas.size(); i++) {
            ProductoImagenRequest imagen = new ProductoImagenRequest();
            imagen.setUrl("/uploads/edit-" + i + ".webp");
            imagen.setSha256(shas.get(i));
            imagen.setClaveArchivoOriginal("productos/edit-" + i + ".jpg");
            imagen.setOrden(i);
            imagenes.add(imagen);
        }

        ProductoVarianteRequest variante = new ProductoVarianteRequest();
        variante.setNombre("Única");
        variante.setPrecio(new BigDecimal("100.00"));
        variante.setStockDisponible(null);
        variante.setOrden(0);
        variante.setUrlImagen("/uploads/edit-0.webp");
        variante.setSha256(shas.get(0));
        variante.setClaveArchivoOriginal("productos/edit-0.jpg");

        DerechoTermino personal = derechoTerminoRepository
            .findByActivoTrueOrderByOrdenAsc().stream()
            .filter(derecho -> "PERSONAL".equals(derecho.getCodigo()))
            .findFirst()
            .orElseThrow();
        LicenciaProductoRequest licencia = new LicenciaProductoRequest();
        licencia.setIdsDerechos(Set.of(personal.getIdDerechoTermino()));
        licencia.setPorcentajeRegalia(BigDecimal.ZERO);

        ProductoRequest request = new ProductoRequest();
        request.setIdTipoProducto(tipo.getIdTipoProducto());
        request.setIdMedio(medio.getIdMedio());
        request.setIdMoneda(moneda.getIdMoneda());
        request.setTitulo("Editable " + UUID.randomUUID());
        request.setDescripcion("Producto para probar la edición de entregables.");
        request.setImagenes(imagenes);
        request.setVariantes(new ArrayList<>(List.of(variante)));
        request.setIdsEtiquetas(Set.of());
        request.setContenidosSensibles(Set.of());
        request.setLicencia(licencia);

        ProductoDTO creado = productoService.crear(artista.getIdUsuario(), request);
        Long idVariante = creado.getVariantes().get(0).getIdVariante();

        // Segunda vuelta: el producto ya tiene entregables guardados.
        ProductoVarianteRequest editada = new ProductoVarianteRequest();
        editada.setIdVariante(idVariante);
        editada.setNombre("Única");
        editada.setPrecio(new BigDecimal("100.00"));
        editada.setStockDisponible(null);
        editada.setOrden(0);
        editada.setUrlImagen("/uploads/edit-0.webp");
        editada.setSha256(shas.get(0));
        editada.setClaveArchivoOriginal("productos/edit-0.jpg");

        ProductoRequest segunda = new ProductoRequest();
        segunda.setIdTipoProducto(tipo.getIdTipoProducto());
        segunda.setIdMedio(medio.getIdMedio());
        segunda.setIdMoneda(moneda.getIdMoneda());
        segunda.setTitulo(creado.getTitulo());
        segunda.setDescripcion("Producto para probar la edición de entregables.");
        segunda.setImagenes(imagenes);
        segunda.setVariantes(new ArrayList<>(List.of(editada)));
        segunda.setIdsEtiquetas(Set.of());
        segunda.setContenidosSensibles(Set.of());
        segunda.setLicencia(licencia);

        ProductoDTO actualizado = productoService.modificar(
            creado.getIdProducto(), artista.getIdUsuario(), segunda);

        List<ProductoEntregableDTO> entregables = actualizado.getEntregables();
        assertThat(entregables).hasSize(1);
        assertThat(entregables.get(0).getSha256()).isEqualTo(shas.get(0));
    }

    @Test
    void cambiarImagenDeVarianteConStock_abreVersionEnElHistorial() {
        Usuario artista = usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getRol() != null
                && usuario.getRol().getNombre() == ERol.ARTISTA)
            .findFirst()
            .orElseThrow();
        TipoProducto tipo = tipoProductoRepository
            .findByActivoTrueOrderByNombreAsc().get(0);
        Medio medio = medioRepository.findByActivoTrueOrderByNombreAsc().stream()
            .filter(m -> m.getTipoEntrega() == TipoEntrega.DIGITAL)
            .findFirst()
            .orElseThrow();
        Moneda moneda = monedaService.predeterminada();
        DerechoTermino personal = derechoTerminoRepository
            .findByActivoTrueOrderByOrdenAsc().stream()
            .filter(derecho -> "PERSONAL".equals(derecho.getCodigo()))
            .findFirst()
            .orElseThrow();

        String sha1 = shaAleatorio();
        String sha2 = shaAleatorio();

        ProductoImagenRequest imagen1 = new ProductoImagenRequest();
        imagen1.setUrl("/uploads/v1-" + UUID.randomUUID() + ".webp");
        imagen1.setSha256(sha1);
        imagen1.setClaveArchivoOriginal("productos/v1-" + UUID.randomUUID() + ".jpg");
        imagen1.setOrden(0);

        ProductoVarianteRequest variante = new ProductoVarianteRequest();
        variante.setNombre("Giclée");
        variante.setPrecio(new BigDecimal("100.00"));
        variante.setStockDisponible(5);
        variante.setOrden(0);
        variante.setUrlImagen(imagen1.getUrl());
        variante.setSha256(sha1);
        variante.setClaveArchivoOriginal(imagen1.getClaveArchivoOriginal());

        LicenciaProductoRequest licencia = new LicenciaProductoRequest();
        licencia.setIdsDerechos(Set.of(personal.getIdDerechoTermino()));
        licencia.setPorcentajeRegalia(BigDecimal.ZERO);

        ProductoRequest request = new ProductoRequest();
        request.setIdTipoProducto(tipo.getIdTipoProducto());
        request.setIdMedio(medio.getIdMedio());
        request.setIdMoneda(moneda.getIdMoneda());
        request.setTitulo("Historial " + UUID.randomUUID());
        request.setDescripcion("Variante con stock que cambia de imagen.");
        request.setImagenes(new ArrayList<>(List.of(imagen1)));
        request.setVariantes(new ArrayList<>(List.of(variante)));
        request.setIdsEtiquetas(Set.of());
        request.setContenidosSensibles(Set.of());
        request.setLicencia(licencia);

        ProductoDTO creado = productoService.crear(artista.getIdUsuario(), request);
        Long idVariante = creado.getVariantes().get(0).getIdVariante();
        assertThat(varianteImagenRepository
            .findByVarianteIdVarianteOrderByNumeroDesc(idVariante)).hasSize(1);

        ProductoImagenRequest imagen2 = new ProductoImagenRequest();
        imagen2.setUrl("/uploads/v2-" + UUID.randomUUID() + ".webp");
        imagen2.setSha256(sha2);
        imagen2.setClaveArchivoOriginal("productos/v2-" + UUID.randomUUID() + ".jpg");
        imagen2.setOrden(0);

        ProductoVarianteRequest editada = new ProductoVarianteRequest();
        editada.setIdVariante(idVariante);
        editada.setNombre("Giclée");
        editada.setPrecio(new BigDecimal("100.00"));
        editada.setStockDisponible(5);
        editada.setOrden(0);
        editada.setUrlImagen(imagen2.getUrl());
        editada.setSha256(sha2);
        editada.setClaveArchivoOriginal(imagen2.getClaveArchivoOriginal());

        ProductoRequest segunda = new ProductoRequest();
        segunda.setIdTipoProducto(tipo.getIdTipoProducto());
        segunda.setIdMedio(medio.getIdMedio());
        segunda.setIdMoneda(moneda.getIdMoneda());
        segunda.setTitulo(creado.getTitulo());
        segunda.setDescripcion("Variante con stock que cambia de imagen.");
        segunda.setImagenes(new ArrayList<>(List.of(imagen2)));
        segunda.setVariantes(new ArrayList<>(List.of(editada)));
        segunda.setIdsEtiquetas(Set.of());
        segunda.setContenidosSensibles(Set.of());
        segunda.setLicencia(licencia);

        productoService.modificar(creado.getIdProducto(), artista.getIdUsuario(), segunda);

        var historial = varianteImagenRepository
            .findByVarianteIdVarianteOrderByNumeroDesc(idVariante);
        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).isVigente()).isTrue();
        assertThat(historial.get(0).getSha256()).isEqualTo(sha2);
        assertThat(historial.get(1).isVigente()).isFalse();
        assertThat(historial.get(1).getSha256()).isEqualTo(sha1);
        assertThat(historial.get(1).getFechaHasta()).isNotNull();
    }

    @Test
    void catalogoPublico_listaPublicadosYExcluyeBorradores() {
        ProductoDTO borrador = crearBorradorSimple();
        ProductoDTO publicado = crearProductoSimple("Catálogo " + UUID.randomUUID());

        List<ProductoCatalogoDTO> lista = productoCatalogoService
            .listarDeArtista(
                usuarioRepository.findByIdUsuario(publicado.getIdArtista())
                    .orElseThrow().getNombreUsuario(),
                null);

        assertThat(lista)
            .extracting(ProductoCatalogoDTO::getIdProducto)
            .contains(publicado.getIdProducto())
            .doesNotContain(borrador.getIdProducto());
    }

    @Test
    void catalogoPublico_noExponeUnBorradorPorId() {
        ProductoDTO borrador = crearBorradorSimple();

        assertThatThrownBy(() -> productoCatalogoService.obtener(
            borrador.getIdProducto(), null))
            .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void catalogoPublico_calculaPrecioDesdeYDisponibilidad() {
        ProductoDTO publicado = crearProductoSimple("Precio " + UUID.randomUUID());

        ProductoCatalogoDTO dto = productoCatalogoService.obtener(
            publicado.getIdProducto(), null);

        assertThat(dto.getPrecioDesde()).isEqualByComparingTo("100.00");
        assertThat(dto.isDisponible()).isTrue();
        assertThat(dto.getCantidadVariantes()).isEqualTo(1);
        assertThat(dto.getIdArtista()).isEqualTo(publicado.getIdArtista());
    }

    private ProductoDTO crearBorradorSimple() {
        Usuario artista = usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getRol() != null
                && usuario.getRol().getNombre() == ERol.ARTISTA)
            .findFirst()
            .orElseThrow();
        String sha = shaAleatorio();
        ProductoImagenRequest imagen = new ProductoImagenRequest();
        imagen.setUrl("/uploads/borrador-" + UUID.randomUUID() + ".webp");
        imagen.setSha256(sha);
        imagen.setClaveArchivoOriginal("productos/borrador-" + UUID.randomUUID() + ".jpg");
        imagen.setOrden(0);
        ProductoVarianteRequest variante = new ProductoVarianteRequest();
        variante.setNombre("Única");
        variante.setOrden(0);
        variante.setUrlImagen(imagen.getUrl());
        variante.setSha256(sha);
        variante.setClaveArchivoOriginal(imagen.getClaveArchivoOriginal());
        ProductoRequest request = new ProductoRequest();
        request.setTitulo("Borrador catálogo " + UUID.randomUUID());
        request.setVariantes(new ArrayList<>(List.of(variante)));
        request.setImagenes(new ArrayList<>(List.of(imagen)));
        return productoService.guardarBorrador(artista.getIdUsuario(), null, request);
    }

    private ProductoDTO crearProductoSimple(String titulo) {
        Usuario artista = usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getRol() != null
                && usuario.getRol().getNombre() == ERol.ARTISTA)
            .findFirst()
            .orElseThrow();
        TipoProducto tipo = tipoProductoRepository
            .findByActivoTrueOrderByNombreAsc().get(0);
        Medio medio = medioRepository.findByActivoTrueOrderByNombreAsc().stream()
            .filter(m -> m.getTipoEntrega() == TipoEntrega.DIGITAL)
            .findFirst()
            .orElseThrow();
        Moneda moneda = monedaService.predeterminada();
        DerechoTermino personal = derechoTerminoRepository
            .findByActivoTrueOrderByOrdenAsc().stream()
            .filter(derecho -> "PERSONAL".equals(derecho.getCodigo()))
            .findFirst()
            .orElseThrow();

        String sha = shaAleatorio();
        ProductoImagenRequest imagen = new ProductoImagenRequest();
        imagen.setUrl("/uploads/cat-" + UUID.randomUUID() + ".webp");
        imagen.setSha256(sha);
        imagen.setClaveArchivoOriginal("productos/cat-" + UUID.randomUUID() + ".jpg");
        imagen.setOrden(0);

        ProductoVarianteRequest variante = new ProductoVarianteRequest();
        variante.setNombre("Única");
        variante.setPrecio(new BigDecimal("100.00"));
        variante.setStockDisponible(null);
        variante.setOrden(0);
        variante.setUrlImagen(imagen.getUrl());
        variante.setSha256(sha);
        variante.setClaveArchivoOriginal(imagen.getClaveArchivoOriginal());

        LicenciaProductoRequest licencia = new LicenciaProductoRequest();
        licencia.setIdsDerechos(Set.of(personal.getIdDerechoTermino()));
        licencia.setPorcentajeRegalia(BigDecimal.ZERO);

        ProductoRequest request = new ProductoRequest();
        request.setIdTipoProducto(tipo.getIdTipoProducto());
        request.setIdMedio(medio.getIdMedio());
        request.setIdMoneda(moneda.getIdMoneda());
        request.setTitulo(titulo);
        request.setDescripcion("Producto de catálogo.");
        request.setImagenes(new ArrayList<>(List.of(imagen)));
        request.setVariantes(new ArrayList<>(List.of(variante)));
        request.setIdsEtiquetas(Set.of());
        request.setContenidosSensibles(Set.of());
        request.setLicencia(licencia);
        return productoService.crear(artista.getIdUsuario(), request);
    }

    private String shaAleatorio() {
        return (UUID.randomUUID().toString() + UUID.randomUUID().toString())
            .replace("-", "");
    }
}
