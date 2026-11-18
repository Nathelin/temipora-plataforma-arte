package com.temipora.backend_springboot.servicios;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoImagen;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraRepository;

/**
 * Descarga de una compra completa. Si hay un solo archivo disponible se
 * devuelve tal cual; si hay varios, se empaquetan en un ZIP. Para las piezas
 * físicas se incluye la imagen pública de la variante, mientras que las
 * digitales usan el archivo original privado.
 */
@Service
public class CompraDescargaService {

    private final CompraRepository compraRepository;
    private final UsuarioService usuarioService;
    private final AlmacenPrivadoService almacenPrivado;
    private final StorageService storageService;

    public CompraDescargaService(
            CompraRepository compraRepository,
            UsuarioService usuarioService,
            AlmacenPrivadoService almacenPrivado,
            StorageService storageService) {
        this.compraRepository = compraRepository;
        this.usuarioService = usuarioService;
        this.almacenPrivado = almacenPrivado;
        this.storageService = storageService;
    }

    public record DescargaCompra(
            Resource recurso,
            String nombreArchivo,
            String contentType) {
    }

    private record ArchivoDescarga(
            String nombre,
            String contentType,
            Resource recurso) {
    }

    @Transactional(readOnly = true)
    public DescargaCompra preparar(String nombreUsuario, Long idCompra) {
        Usuario cliente = usuarioService.getByNombreUsuario(nombreUsuario);
        Compra compra = compraRepository
            .findByIdCompraAndClienteIdUsuario(
                idCompra, cliente.getIdUsuario())
            .orElseThrow(() -> new AccesoDenegadoException(
                "No tenés acceso a esta compra."));

        List<ArchivoDescarga> archivos = new ArrayList<>();
        Set<String> nombres = new HashSet<>();
        for (CompraGrupo grupo : compra.getGrupos()) {
            for (CompraItem item : grupo.getItems()) {
                ArchivoDescarga archivo = archivoDe(item, nombres);
                if (archivo != null) {
                    archivos.add(archivo);
                }
            }
        }

        if (archivos.isEmpty()) {
            throw new ConflictoException(
                "Todavía no hay archivos disponibles para descargar.");
        }
        if (archivos.size() == 1) {
            ArchivoDescarga unico = archivos.get(0);
            return new DescargaCompra(
                unico.recurso(), unico.nombre(), unico.contentType());
        }
        return new DescargaCompra(
            zip(archivos),
            "compra-" + idCompra + ".zip",
            "application/zip");
    }

    private ArchivoDescarga archivoDe(
            CompraItem item, Set<String> nombres) {
        if (item.getEstadoEntrega() != EstadoEntregaItem.ENTREGADO) {
            return null;
        }
        if (item.getTipoEntrega() == TipoEntrega.DIGITAL) {
            String clave = item.getVariante() != null
                ? item.getVariante().getClaveArchivoOriginal() : null;
            if (clave == null || clave.isBlank()) {
                return null;
            }
            try {
                return new ArchivoDescarga(
                    nombre(item, extension(clave), nombres),
                    mediaType(clave),
                    almacenPrivado.abrir(clave));
            } catch (NoSuchElementException e) {
                return null;
            }
        }

        String imagen = imagenPublica(item);
        if (imagen == null) {
            return null;
        }
        try {
            return new ArchivoDescarga(
                nombre(item, extension(imagen), nombres),
                mediaType(imagen),
                storageService.abrir(imagen));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private String imagenPublica(CompraItem item) {
        ProductoVariante variante = item.getVariante();
        if (variante != null && hayTexto(variante.getImagenUrl())) {
            return variante.getImagenUrl();
        }
        Producto producto = item.getProducto();
        if (producto != null) {
            if (hayTexto(producto.getImagenPortada())) {
                return producto.getImagenPortada();
            }
            if (producto.getImagenes() != null) {
                return producto.getImagenes().stream()
                    .filter(imagen -> imagen != null && hayTexto(imagen.getUrl()))
                    .sorted(Comparator.comparing(ProductoImagen::getOrden,
                        Comparator.nullsLast(Integer::compareTo)))
                    .map(ProductoImagen::getUrl)
                    .findFirst()
                    .orElse(null);
            }
        }
        return null;
    }

    private String nombre(
            CompraItem item, String extension, Set<String> usados) {
        String base = hayTexto(item.getTituloProducto())
            ? item.getTituloProducto()
            : "producto";
        if (hayTexto(item.getNombreVariante())) {
            base += "-" + item.getNombreVariante();
        }
        base = base.replaceAll("[\\\\/:*?\"<>|]", "-").trim();
        if (base.isEmpty()) {
            base = "producto";
        }
        String nombre = base + extension;
        int numero = 2;
        while (!usados.add(nombre)) {
            nombre = base + " (" + numero + ")" + extension;
            numero++;
        }
        return nombre;
    }

    private Resource zip(List<ArchivoDescarga> archivos) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(
                    salida, StandardCharsets.UTF_8)) {
            for (ArchivoDescarga archivo : archivos) {
                zip.putNextEntry(new ZipEntry(archivo.nombre()));
                try (InputStream entrada = archivo.recurso().getInputStream()) {
                    entrada.transferTo(zip);
                }
                zip.closeEntry();
            }
            zip.finish();
            return new ByteArrayResource(salida.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("No se pudo preparar la descarga.", e);
        }
    }

    private String mediaType(String nombre) {
        String minuscula = nombre.toLowerCase();
        if (minuscula.endsWith(".png")) {
            return "image/png";
        }
        if (minuscula.endsWith(".jpg") || minuscula.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (minuscula.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto >= 0 ? nombre.substring(punto) : "";
    }

    private boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
