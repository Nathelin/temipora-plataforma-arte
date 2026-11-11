package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraRepository;

@ExtendWith(MockitoExtension.class)
class CompraDescargaServiceTest {

    @Mock private CompraRepository compraRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private AlmacenPrivadoService almacenPrivado;
    @Mock private StorageService storageService;

    @InjectMocks private CompraDescargaService service;

    @Test
    void preparar_unSoloArchivoDigital_devuelveElArchivoDirecto() {
        Usuario cliente = usuario(1L, "cliente");
        Compra compra = compra(cliente);
        compra.getGrupos().get(0).getItems().add(
            itemDigital(compra, 1L, "Pieza", "Única", "productos/a.png"));
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraRepository.findByIdCompraAndClienteIdUsuario(10L, 1L))
            .thenReturn(Optional.of(compra));
        when(almacenPrivado.abrir("productos/a.png"))
            .thenReturn(new ByteArrayResource("contenido".getBytes(StandardCharsets.UTF_8)));

        CompraDescargaService.DescargaCompra descarga =
            service.preparar("cliente", 10L);

        assertThat(descarga.nombreArchivo()).isEqualTo("Pieza-Única.png");
        assertThat(descarga.contentType()).isEqualTo("image/png");
    }

    @Test
    void preparar_variosArchivos_armaUnZipConDigitalYFisica() throws Exception {
        Usuario cliente = usuario(1L, "cliente");
        Compra compra = compra(cliente);
        CompraGrupo grupo = compra.getGrupos().get(0);
        grupo.getItems().add(
            itemDigital(compra, 1L, "Pieza", "Única", "productos/a.png"));
        grupo.getItems().add(
            itemFisica(compra, 2L, "Obra física", "Original", "/uploads/u.webp"));

        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraRepository.findByIdCompraAndClienteIdUsuario(10L, 1L))
            .thenReturn(Optional.of(compra));
        when(almacenPrivado.abrir("productos/a.png"))
            .thenReturn(new ByteArrayResource("digital".getBytes(StandardCharsets.UTF_8)));
        when(storageService.abrir("/uploads/u.webp"))
            .thenReturn(new ByteArrayResource("imagen".getBytes(StandardCharsets.UTF_8)));

        CompraDescargaService.DescargaCompra descarga =
            service.preparar("cliente", 10L);

        assertThat(descarga.nombreArchivo()).isEqualTo("compra-10.zip");
        assertThat(descarga.contentType()).isEqualTo("application/zip");

        List<String> nombres = new ArrayList<>();
        try (InputStream entrada = descarga.recurso().getInputStream();
                ZipInputStream zip = new ZipInputStream(
                    entrada, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                nombres.add(entry.getName());
            }
        }
        assertThat(nombres)
            .containsExactlyInAnyOrder("Pieza-Única.png", "Obra física-Original.webp");
    }

    @Test
    void preparar_unSoloArchivoFisico_devuelveLaImagen() {
        Usuario cliente = usuario(1L, "cliente");
        Compra compra = compra(cliente);
        compra.getGrupos().get(0).getItems().add(
            itemFisica(compra, 1L, "Obra", "Original", "/uploads/u.webp"));
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraRepository.findByIdCompraAndClienteIdUsuario(10L, 1L))
            .thenReturn(Optional.of(compra));
        when(storageService.abrir("/uploads/u.webp"))
            .thenReturn(new ByteArrayResource("imagen".getBytes(StandardCharsets.UTF_8)));

        CompraDescargaService.DescargaCompra descarga =
            service.preparar("cliente", 10L);

        assertThat(descarga.nombreArchivo()).isEqualTo("Obra-Original.webp");
        assertThat(descarga.contentType()).isEqualTo("image/webp");
    }

    @Test
    void preparar_sinClaveNiImagen_lanzaConflicto() {
        Usuario cliente = usuario(1L, "cliente");
        Compra compra = compra(cliente);
        CompraGrupo grupo = compra.getGrupos().get(0);
        grupo.getItems().add(
            itemDigital(compra, 1L, "Digital", "Única", null));
        grupo.getItems().add(
            itemFisica(compra, 2L, "Física", "Original", null));

        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraRepository.findByIdCompraAndClienteIdUsuario(10L, 1L))
            .thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> service.preparar("cliente", 10L))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("archivos disponibles");
    }

    @Test
    void preparar_compraAjena_lanzaAccesoDenegado() {
        Usuario cliente = usuario(1L, "cliente");
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraRepository.findByIdCompraAndClienteIdUsuario(10L, 1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preparar("cliente", 10L))
            .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void preparar_sinArchivosDisponibles_lanzaConflicto() {
        Usuario cliente = usuario(1L, "cliente");
        Compra compra = compra(cliente);
        CompraItem pendiente = itemDigital(
            compra, 1L, "Pieza", "Única", "productos/a.png");
        pendiente.setEstadoEntrega(EstadoEntregaItem.PENDIENTE);
        compra.getGrupos().get(0).getItems().add(pendiente);

        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraRepository.findByIdCompraAndClienteIdUsuario(10L, 1L))
            .thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> service.preparar("cliente", 10L))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("archivos disponibles");
    }

    private Compra compra(Usuario cliente) {
        Compra compra = new Compra();
        compra.setIdCompra(10L);
        compra.setCliente(cliente);
        CompraGrupo grupo = new CompraGrupo();
        grupo.setCompra(compra);
        compra.getGrupos().add(grupo);
        return compra;
    }

    private CompraItem itemDigital(
            Compra compra,
            Long id,
            String titulo,
            String variante,
            String clave) {
        Producto producto = producto(compra, id, titulo);
        ProductoVariante productoVariante = new ProductoVariante();
        productoVariante.setIdVariante(id + 100);
        productoVariante.setProducto(producto);
        productoVariante.setClaveArchivoOriginal(clave);
        productoVariante.setImagenUrl("/uploads/u.webp");

        CompraItem item = new CompraItem();
        item.setIdCompraItem(id);
        item.setGrupo(compra.getGrupos().get(0));
        item.setProducto(producto);
        item.setVariante(productoVariante);
        item.setTipoEntrega(TipoEntrega.DIGITAL);
        item.setEstadoEntrega(EstadoEntregaItem.ENTREGADO);
        item.setTituloProducto(titulo);
        item.setNombreVariante(variante);
        return item;
    }

    private CompraItem itemFisica(
            Compra compra,
            Long id,
            String titulo,
            String variante,
            String imagen) {
        Producto producto = producto(compra, id, titulo);
        ProductoVariante productoVariante = new ProductoVariante();
        productoVariante.setIdVariante(id + 100);
        productoVariante.setProducto(producto);
        productoVariante.setImagenUrl(imagen);

        CompraItem item = new CompraItem();
        item.setIdCompraItem(id);
        item.setGrupo(compra.getGrupos().get(0));
        item.setProducto(producto);
        item.setVariante(productoVariante);
        item.setTipoEntrega(TipoEntrega.FISICA);
        item.setEstadoEntrega(EstadoEntregaItem.ENTREGADO);
        item.setTituloProducto(titulo);
        item.setNombreVariante(variante);
        return item;
    }

    private Producto producto(Compra compra, Long id, String titulo) {
        Producto producto = new Producto();
        producto.setIdProducto(id + 200);
        producto.setTitulo(titulo);
        producto.setArtista(compra.getCliente());
        return producto;
    }

    private Usuario usuario(Long id, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombre);
        return usuario;
    }
}
