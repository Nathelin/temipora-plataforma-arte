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
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.EventoLedger;
import com.temipora.backend_springboot.modelo.LicenciaProducto;
import com.temipora.backend_springboot.modelo.LicenciaProductoRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoDTO;
import com.temipora.backend_springboot.modelo.ProductoImagenRequest;
import com.temipora.backend_springboot.modelo.ProductoRequest;
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
        String shaImagen = shaAleatorio();
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

        ProductoImagenRequest imagen = new ProductoImagenRequest();
        imagen.setUrl("/uploads/test-producto.webp");
        imagen.setSha256(shaImagen);
        imagen.setClaveArchivoOriginal("productos/test-producto.webp");
        imagen.setOrden(0);

        ProductoRequest request = new ProductoRequest();
        request.setIdTipoProducto(tipo.getIdTipoProducto());
        request.setIdMedio(medio.getIdMedio());
        request.setIdMoneda(moneda.getIdMoneda());
        request.setTitulo(titulo);
        request.setDescripcion("Producto de prueba de integración.");
        request.setImagenes(new ArrayList<>(List.of(imagen)));
        request.setVariantes(new ArrayList<>(List.of(variante)));
        request.setIdsEtiquetas(Set.of());
        request.setContenidosSensibles(Set.of());

        DerechoTermino personal = derechoTerminoRepository
            .findByActivoTrueOrderByOrdenAsc().stream()
            .filter(derecho -> "PERSONAL".equals(derecho.getCodigo()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No existe el derecho PERSONAL en el catálogo."));
        LicenciaProductoRequest licencia = new LicenciaProductoRequest();
        licencia.setIdsDerechos(Set.of(personal.getIdDerechoTermino()));
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
        assertThat(dto.getPrecioDesde()).isEqualByComparingTo("150.00");
        assertThat(dto.getTipoEntregaMedio()).isEqualTo(TipoEntrega.DIGITAL);

        assertThat(dto.getLicencia()).isNotNull();
        assertThat(dto.getLicencia().getDerechos()).hasSize(1);
        assertThat(dto.getLicencia().getDerechos().get(0).getCodigo())
            .isEqualTo("PERSONAL");

        Producto producto = productoRepository.findByIdProducto(dto.getIdProducto())
            .orElseThrow();
        assertThat(producto.getVariantes()).hasSize(1);
        assertThat(producto.getImagenes()).hasSize(1);
        assertThat(producto.getLicencias()).hasSize(1);
        LicenciaProducto licenciaGuardada = producto.licenciaVigente();
        assertThat(licenciaGuardada).isNotNull();
        assertThat(licenciaGuardada.getDerechos()).hasSize(1);
        assertThat(licenciaGuardada.isRequiereCredito()).isTrue();
        Long idVariante = producto.getVariantes().get(0).getIdVariante();

        List<EventoLedger> eventos = eventoLedgerRepository
            .findByVarianteIdVarianteOrderBySecuenciaAsc(idVariante);
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).getTipo()).isEqualTo(TipoEventoLedger.GENESIS);
        assertThat(eventos.get(0).getHashEvento()).hasSize(64);
        assertThat(eventos.get(0).getHashImagen()).isEqualTo(shaVariante);

        assertThat(ledgerService.verificarProducto(producto).isIntegridad()).isTrue();
    }

    private String shaAleatorio() {
        return (UUID.randomUUID().toString() + UUID.randomUUID().toString())
            .replace("-", "");
    }
}
