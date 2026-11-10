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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EventoLedger;
import com.temipora.backend_springboot.modelo.EventoLedgerDTO;
import com.temipora.backend_springboot.modelo.LicenciaProductoRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.ProductoDTO;
import com.temipora.backend_springboot.modelo.ProductoImagenRequest;
import com.temipora.backend_springboot.modelo.ProductoRequest;
import com.temipora.backend_springboot.modelo.ProductoVarianteRequest;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.VarianteLedgerDTO;
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.TipoProductoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.LedgerService;
import com.temipora.backend_springboot.servicios.MonedaService;
import com.temipora.backend_springboot.servicios.ProductoService;

/**
 * El ledger es la promesa central de la plataforma: el historial de una pieza
 * única no puede alterarse. Estos tests verifican que la base lo impida y que
 * la manipulación del contenido se detecte.
 */
@SpringBootTest
@Transactional
class LedgerIntegridadTest {

    @Autowired private ProductoService productoService;
    @Autowired private LedgerService ledgerService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoProductoRepository tipoProductoRepository;
    @Autowired private MedioRepository medioRepository;
    @Autowired private MonedaService monedaService;
    @Autowired private DerechoTerminoRepository derechoTerminoRepository;
    @Autowired private EventoLedgerRepository eventoLedgerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    @Test
    void genesisNuevoSeEmiteEnVersion2ConHashDeDatos() {
        ProductoDTO dto = crearProductoUnico();
        Long idVariante = idVarianteDe(dto);

        VarianteLedgerDTO ledger = verificar(idVariante, dto);

        assertThat(ledger.isIntegridad()).isTrue();
        EventoLedgerDTO genesis = ledger.getEventos().get(0);
        assertThat(genesis.getVersionHash()).isEqualTo(EventoLedger.VERSION_ACTUAL);

        EventoLedger entidad = eventoLedgerRepository
            .findByVarianteIdVarianteOrderBySecuenciaAsc(idVariante)
            .get(0);
        assertThat(entidad.getHashDatos()).hasSize(64);
        assertThat(entidad.getHashDatos()).isEqualTo(sha256(entidad.getDatos()));
    }

    @Test
    void alterarLaColumnaDatosRompeLaVerificacion() {
        ProductoDTO dto = crearProductoUnico();
        Long idVariante = idVarianteDe(dto);

        VarianteLedgerDTO antes = verificar(idVariante, dto);
        assertThat(antes.isIntegridad()).isTrue();

        // El trigger impide el UPDATE, así que se desactiva para simular a
        // alguien con acceso total (superusuario o trigger deshabilitado).
        // Es la única forma de comprobar que el hash detecta la alteración:
        // si no la detectara, el trigger sería la única defensa.
        jdbcTemplate.execute(
            "ALTER TABLE evento_ledger DISABLE TRIGGER trg_evento_ledger_inmutable");
        try {
            jdbcTemplate.update(
                "UPDATE evento_ledger SET datos = ? WHERE id_variante = ?",
                "{\"titulo\":\"Pieza falsificada\"}", idVariante);

            // El UPDATE fue por JDBC y no pasa por Hibernate: hay que descartar
            // la entidad cacheada en la sesión para leer el valor real.
            entityManager.clear();

            VarianteLedgerDTO despues = verificar(idVariante, dto);
            assertThat(despues.isIntegridad()).isFalse();
            assertThat(despues.getMensaje()).contains("hash de datos no coincide");
        } finally {
            jdbcTemplate.execute(
                "ALTER TABLE evento_ledger ENABLE TRIGGER trg_evento_ledger_inmutable");
        }
    }

    @Test
    void noSePuedeActualizarUnEventoDelLedger() {
        ProductoDTO dto = crearProductoUnico();
        Long idVariante = idVarianteDe(dto);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "UPDATE evento_ledger SET monto = 1 WHERE id_variante = ?",
            idVariante))
            .hasMessageContaining("inmutable");
    }

    @Test
    void noSePuedeBorrarUnEventoDelLedger() {
        ProductoDTO dto = crearProductoUnico();
        Long idVariante = idVarianteDe(dto);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "DELETE FROM evento_ledger WHERE id_variante = ?",
            idVariante))
            .hasMessageContaining("inmutable");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Long idVarianteDe(ProductoDTO dto) {
        return productoService.obtenerEntidadPropia(
            dto.getIdProducto(), dto.getIdArtista())
            .getVariantes().iterator().next().getIdVariante();
    }

    private VarianteLedgerDTO verificar(Long idVariante, ProductoDTO dto) {
        return ledgerService.verificarVariante(
            productoService.obtenerEntidadPropia(
                dto.getIdProducto(), dto.getIdArtista())
                .getVariantes().iterator().next());
    }

    private ProductoDTO crearProductoUnico() {
        Usuario artista = usuarioRepository.findAll().stream()
            .filter(usuario -> usuario.getRol() != null
                && usuario.getRol().getNombre() == ERol.ARTISTA)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No hay un usuario ARTISTA para la prueba."));
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
        imagen.setUrl("/uploads/" + UUID.randomUUID() + ".webp");
        imagen.setSha256(sha);
        imagen.setClaveArchivoOriginal("productos/" + UUID.randomUUID() + ".jpg");
        imagen.setOrden(0);

        ProductoVarianteRequest variante = new ProductoVarianteRequest();
        variante.setNombre("Única");
        variante.setPrecio(new BigDecimal("100.00"));
        variante.setStockDisponible(1);
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
        request.setTitulo("Ledger " + UUID.randomUUID());
        request.setDescripcion("Pieza única para probar el ledger.");
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

    private String sha256(String valor) {
        try {
            return java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
