package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.CambioAuditoriaDTO;
import com.temipora.backend_springboot.modelo.ContenidoSensible;
import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.LicenciaProducto;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoImagen;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoProducto;

class SnapshotProductoTest {

    private static final String SHA_A = "a".repeat(64);
    private static final String SHA_B = "b".repeat(64);

    @Test
    void sinCambios_noDevuelveFilas() {
        Producto producto = producto();
        producto.agregarVariante(variante(1L, "Original", "100.00", 5, SHA_A));

        SnapshotProducto antes = SnapshotProducto.de(producto);

        assertThat(antes.diferencias(producto)).isEmpty();
    }

    @Test
    void cambioDePrecioYStockDeUnaVariante_seReporta() {
        Producto producto = producto();
        ProductoVariante variante = variante(1L, "Original", "100.00", 5, SHA_A);
        producto.agregarVariante(variante);
        SnapshotProducto antes = SnapshotProducto.de(producto);

        variante.setPrecio(new BigDecimal("150.00"));
        variante.setStockDisponible(4);
        List<CambioAuditoriaDTO> cambios = antes.diferencias(producto);

        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Precio · «Original»");
            assertThat(cambio.anterior()).isEqualTo("100.00 ARS");
            assertThat(cambio.nuevo()).isEqualTo("150.00 ARS");
        });
        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Stock · «Original»");
            assertThat(cambio.anterior()).isEqualTo("5");
            assertThat(cambio.nuevo()).isEqualTo("4");
        });
    }

    @Test
    void varianteNuevaYQuitada_seReportan() {
        Producto producto = producto();
        ProductoVariante original = variante(1L, "Original", "100.00", 5, SHA_A);
        producto.agregarVariante(original);
        SnapshotProducto antes = SnapshotProducto.de(producto);

        producto.quitarVariante(original);
        producto.agregarVariante(variante(2L, "Nueva", "200.00", 3, SHA_B));
        List<CambioAuditoriaDTO> cambios = antes.diferencias(producto);

        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Variante «Nueva»");
            assertThat(cambio.anterior()).isNull();
            assertThat(cambio.nuevo())
                .contains("agregada")
                .contains("200.00 ARS")
                .contains("stock 3");
        });
        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Variante «Original»");
            assertThat(cambio.nuevo()).isEqualTo("quitada");
        });
    }

    @Test
    void varianteArchivada_seMarcaComoQuitadaDeLaPublicacion() {
        Producto producto = producto();
        ProductoVariante variante = variante(1L, "Única", "100.00", 1, SHA_A);
        producto.agregarVariante(variante);
        SnapshotProducto antes = SnapshotProducto.de(producto);

        variante.setEstado(EstadoVariante.ARCHIVADO);
        List<CambioAuditoriaDTO> cambios = antes.diferencias(producto);

        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Estado · «Única»");
            assertThat(cambio.nuevo())
                .isEqualTo("ARCHIVADO (quitada de la publicación)");
        });
    }

    @Test
    void cambiarSoloElOrden_noGeneraCambios() {
        Producto producto = producto();
        ProductoVariante variante = variante(1L, "Original", "100.00", 5, SHA_A);
        producto.agregarVariante(variante);
        SnapshotProducto antes = SnapshotProducto.de(producto);

        variante.setOrden(7);

        assertThat(antes.diferencias(producto)).isEmpty();
    }

    @Test
    void nuevaVersionDeLicencia_seReportaConSusCampos() {
        Producto producto = producto();
        LicenciaProducto v1 = licencia(1, true, "Contenido", "0.00");
        producto.agregarLicencia(v1);
        SnapshotProducto antes = SnapshotProducto.de(producto);

        v1.setVigente(false);
        LicenciaProducto v2 = licencia(2, true, "Reventa", "5.00");
        v2.setPermiteModificaciones(true);
        producto.agregarLicencia(v2);
        List<CambioAuditoriaDTO> cambios = antes.diferencias(producto);

        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Licencia");
            assertThat(cambio.anterior()).isEqualTo("v1");
            assertThat(cambio.nuevo()).isEqualTo("v2");
        });
        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Derechos");
            assertThat(cambio.anterior()).isEqualTo("Contenido");
            assertThat(cambio.nuevo()).isEqualTo("Reventa");
        });
        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Permite modificaciones");
            assertThat(cambio.anterior()).isEqualTo("No");
            assertThat(cambio.nuevo()).isEqualTo("Sí");
        });
        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Regalía");
            assertThat(cambio.nuevo()).isEqualTo("5.00%");
        });
    }

    @Test
    void etiquetasAgregadasYQuitadas_seReportan() {
        Producto producto = producto();
        producto.reemplazarEtiquetas(Set.of(etiqueta(1L, "Boceto")));
        SnapshotProducto antes = SnapshotProducto.de(producto);

        producto.reemplazarEtiquetas(Set.of(etiqueta(2L, "Fantasía")));
        List<CambioAuditoriaDTO> cambios = antes.diferencias(producto);

        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Etiquetas agregadas");
            assertThat(cambio.nuevo()).isEqualTo("Fantasía");
        });
        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Etiquetas quitadas");
            assertThat(cambio.anterior()).isEqualTo("Boceto");
        });
    }

    @Test
    void imagenesAgregadasYQuitadas_seReportanPorOrden() {
        Producto producto = producto();
        producto.reemplazarImagenes(new ArrayList<>(List.of(imagen(SHA_A, 0))));
        SnapshotProducto antes = SnapshotProducto.de(producto);

        producto.reemplazarImagenes(
            new ArrayList<>(List.of(imagen(SHA_A, 0), imagen(SHA_B, 1))));
        List<CambioAuditoriaDTO> cambios = antes.diferencias(producto);

        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Imagen (orden 1)");
            assertThat(cambio.anterior()).isNull();
            assertThat(cambio.nuevo()).isEqualTo("agregada");
        });
    }

    @Test
    void tiposDeContenidoSensible_seReportan() {
        Producto producto = producto();
        SnapshotProducto antes = SnapshotProducto.de(producto);

        producto.reemplazarContenidosSensibles(Set.of(ContenidoSensible.VIOLENCIA));
        List<CambioAuditoriaDTO> cambios = antes.diferencias(producto);

        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Tipos de contenido sensible");
            assertThat(cambio.anterior()).isNull();
            assertThat(cambio.nuevo()).isEqualTo("VIOLENCIA");
        });
    }

    @Test
    void textoLargo_seTruncaEnElEvento() {
        Producto producto = producto();
        producto.setDescripcion("corta");
        SnapshotProducto antes = SnapshotProducto.de(producto);

        producto.setDescripcion("x".repeat(300));
        List<CambioAuditoriaDTO> cambios = antes.diferencias(producto);

        assertThat(cambios).anySatisfy(cambio -> {
            assertThat(cambio.etiqueta()).isEqualTo("Descripción");
            assertThat(cambio.nuevo()).hasSize(141).endsWith("…");
        });
    }

    private Producto producto() {
        Producto producto = new Producto();
        producto.setTitulo("Plancha");
        producto.setDescripcion("Una plancha");
        producto.setEstado(EstadoProducto.DISPONIBLE);
        producto.setMoneda(moneda());
        producto.setTipoProducto(tipo());
        producto.setMedio(medio());
        producto.setContenidosSensibles(new HashSet<>());
        return producto;
    }

    private ProductoVariante variante(
            Long id, String nombre, String precio, int stock, String sha) {
        ProductoVariante variante = new ProductoVariante();
        variante.setIdVariante(id);
        variante.setNombre(nombre);
        variante.setDescripcion("desc " + nombre);
        variante.setPrecio(new BigDecimal(precio));
        variante.setStockDisponible(stock);
        variante.setEstado(EstadoVariante.DISPONIBLE);
        variante.setNumeroEdicion(1);
        variante.setOrden(0);
        variante.setSha256(sha);
        return variante;
    }

    private LicenciaProducto licencia(
            int version, boolean vigente, String derecho, String regalia) {
        LicenciaProducto licencia = new LicenciaProducto();
        licencia.setVersion(version);
        licencia.setVigente(vigente);
        licencia.setPermiteModificaciones(false);
        licencia.setRequiereCredito(false);
        licencia.setPorcentajeRegalia(new BigDecimal(regalia));
        licencia.reemplazarDerechos(Set.of(derecho(derecho)));
        return licencia;
    }

    private DerechoTermino derecho(String nombre) {
        DerechoTermino derecho = new DerechoTermino();
        derecho.setIdDerechoTermino(1L);
        derecho.setCodigo(nombre.toUpperCase());
        derecho.setNombre(nombre);
        derecho.setOrden(0);
        derecho.setActivo(true);
        derecho.setRequierePiezaUnica(false);
        return derecho;
    }

    private Etiqueta etiqueta(Long id, String nombre) {
        Etiqueta etiqueta = new Etiqueta();
        etiqueta.setIdEtiqueta(id);
        etiqueta.setNombre(nombre);
        return etiqueta;
    }

    private ProductoImagen imagen(String sha, int orden) {
        ProductoImagen imagen = new ProductoImagen();
        imagen.setUrl("imagen-" + orden + ".webp");
        imagen.setSha256(sha);
        imagen.setOrden(orden);
        imagen.setClaveArchivoOriginal("productos/imagen-" + orden + ".webp");
        return imagen;
    }

    private Moneda moneda() {
        Moneda moneda = new Moneda();
        moneda.setIdMoneda(1L);
        moneda.setCodigo("ARS");
        moneda.setNombre("Peso argentino");
        return moneda;
    }

    private TipoProducto tipo() {
        TipoProducto tipo = new TipoProducto();
        tipo.setIdTipoProducto(1L);
        tipo.setNombre("Adoptable");
        return tipo;
    }

    private Medio medio() {
        Medio medio = new Medio();
        medio.setIdMedio(1L);
        medio.setNombre("Digital");
        return medio;
    }
}
