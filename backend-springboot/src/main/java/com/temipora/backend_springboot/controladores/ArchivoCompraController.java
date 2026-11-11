package com.temipora.backend_springboot.controladores;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.servicios.AccesoDenegadoException;
import com.temipora.backend_springboot.servicios.AlmacenPrivadoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api/compras")
@PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
public class ArchivoCompraController {

    private final CompraItemRepository compraItemRepository;
    private final UsuarioService usuarioService;
    private final AlmacenPrivadoService almacen;

    public ArchivoCompraController(
            CompraItemRepository compraItemRepository,
            UsuarioService usuarioService,
            AlmacenPrivadoService almacen) {
        this.compraItemRepository = compraItemRepository;
        this.usuarioService = usuarioService;
        this.almacen = almacen;
    }

    @GetMapping("/{idCompra}/items/{idItem}/archivo")
    public ResponseEntity<Resource> archivo(
            @PathVariable Long idCompra,
            @PathVariable Long idItem,
            Authentication authentication) {
        Usuario cliente = usuarioService.getByNombreUsuario(authentication.getName());
        CompraItem item = compraItemRepository
            .findByIdCompraItemAndGrupoCompraClienteIdUsuario(
                idItem, cliente.getIdUsuario())
            .orElseThrow(() -> new AccesoDenegadoException(
                "No tenés acceso a este archivo."));
        if (item.getGrupo().getCompra().getIdCompra() == null
                || !item.getGrupo().getCompra().getIdCompra().equals(idCompra)) {
            throw new AccesoDenegadoException("No tenés acceso a este archivo.");
        }
        if (item.getTipoEntrega() != TipoEntrega.DIGITAL
                || item.getEstadoEntrega() != EstadoEntregaItem.ENTREGADO) {
            throw new AccesoDenegadoException(
                "El archivo se habilita cuando se acredita el pago.");
        }
        String clave = item.getVariante() != null
            ? item.getVariante().getClaveArchivoOriginal() : null;
        if (clave == null || clave.isBlank()) {
            throw new AccesoDenegadoException(
                "El archivo original no está disponible.");
        }

        String nombre = item.getTituloProducto()
            + (item.getNombreVariante() != null
                ? "-" + item.getNombreVariante() : "")
            + extension(clave);
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(nombre, StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .contentType(mediaType(clave))
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(almacen.abrir(clave));
    }

    private MediaType mediaType(String clave) {
        String minuscula = clave.toLowerCase();
        if (minuscula.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (minuscula.endsWith(".jpg") || minuscula.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (minuscula.endsWith(".webp")) {
            try {
                return MediaType.parseMediaType("image/webp");
            } catch (InvalidMediaTypeException e) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String extension(String clave) {
        int punto = clave.lastIndexOf('.');
        return punto >= 0 ? clave.substring(punto) : "";
    }
}
