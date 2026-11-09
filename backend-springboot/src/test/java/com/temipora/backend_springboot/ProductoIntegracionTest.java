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

import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.EventoLedger;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoDTO;
import com.temipora.backend_springboot.modelo.ProductoImagenRequest;
import com.temipora.backend_springboot.modelo.ProductoRequest;
import com.temipora.backend_springboot.modelo.ProductoVarianteRequest;
import com.temipora.backend_springboot.modelo.TipoEventoLedger;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.TipoProductoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.LedgerService;
import com.temipora.backend_springboot.servicios.MonedaService;
import com.temipora.backend_springboot.servicios.ProductoService;
import com.temipora.backend_springboot.servicios.TerminosService;

@SpringBootTest
@Transactional
class ProductoIntegracionTest {

    @Autowired private ProductoService productoService;
    @Autowired private LedgerService ledgerService;
    @Autowired private TerminosService terminosService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoProductoRepository tipoProductoRepository;
    @Autowired private MedioRepository medioRepository;
    @Autowired private MonedaService monedaService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private EventoLedgerRepository eventoLedgerRepository;

    @Test
    void crearProductoUnico_persisteVariantesTerminosYGenesis() {
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
        com.temipora.backend_springboot.modelo.TerminosRequest terminos =
            new com.temipora.backend_springboot.modelo.TerminosRequest();
        terminos.setUsoCliente("Uso personal de la pieza.");
        request.setTerminos(terminos);

        ProductoDTO dto = productoService.crear(artista.getIdUsuario(), request);

        assertThat(dto.getIdProducto()).isNotNull();
        assertThat(dto.getTitulo()).isEqualTo(titulo);
        assertThat(dto.getVariantes()).hasSize(1);
        assertThat(dto.getVariantes().get(0).isTieneLedger()).isTrue();
        assertThat(dto.getPrecioDesde()).isEqualByComparingTo("150.00");

        Producto producto = productoRepository.findByIdProducto(dto.getIdProducto())
            .orElseThrow();
        assertThat(producto.getVariantes()).hasSize(1);
        assertThat(producto.getImagenes()).hasSize(1);
        Long idVariante = producto.getVariantes().get(0).getIdVariante();

        List<EventoLedger> eventos = eventoLedgerRepository
            .findByVarianteIdVarianteOrderBySecuenciaAsc(idVariante);
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).getTipo()).isEqualTo(TipoEventoLedger.GENESIS);
        assertThat(eventos.get(0).getHashEvento()).hasSize(64);
        assertThat(eventos.get(0).getHashImagen()).isEqualTo(shaVariante);

        assertThat(ledgerService.verificarProducto(producto).isIntegridad()).isTrue();
        assertThat(terminosService
            .obtenerProducto(dto.getIdProducto(), artista.getIdUsuario())
            .getIdProducto())
            .isEqualTo(dto.getIdProducto());
    }

    private String shaAleatorio() {
        return (UUID.randomUUID().toString() + UUID.randomUUID().toString())
            .replace("-", "");
    }
}
