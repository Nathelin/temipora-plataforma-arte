package com.temipora.backend_springboot.controladores;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

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

import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.servicios.AccesoDenegadoException;
import com.temipora.backend_springboot.servicios.EntregaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api/pedidos")
public class ArchivoPedidoController {

    private final EntregaService entregaService;
    private final UsuarioService usuarioService;

    public ArchivoPedidoController(
            EntregaService entregaService,
            UsuarioService usuarioService) {
        this.entregaService = entregaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/{idPedido}/entregas/{idEntrega}/archivos/{idArchivo}/original")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA', 'ADMIN')")
    public ResponseEntity<Resource> original(
            @PathVariable Long idPedido,
            @PathVariable Long idEntrega,
            @PathVariable Long idArchivo,
            Authentication authentication) {
        Acceso acceso = cargarAutorizado(
            idPedido, idEntrega, idArchivo, authentication);

        if (!entregaService.puedeVerOriginal(
                acceso.archivo(), acceso.esArtista(), acceso.esAdmin())) {
            throw new AccesoDenegadoException(
                "El archivo final se libera al pagar la última cuota.");
        }

        return respuesta(
            entregaService.abrir(acceso.archivo()),
            mediaType(acceso.archivo().getMime()),
            acceso.archivo().getNombreOriginal(),
            true);
    }

    @GetMapping("/{idPedido}/entregas/{idEntrega}/archivos/{idArchivo}/preview")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA', 'ADMIN')")
    public ResponseEntity<Resource> preview(
            @PathVariable Long idPedido,
            @PathVariable Long idEntrega,
            @PathVariable Long idArchivo,
            Authentication authentication) {
        Acceso acceso = cargarAutorizado(
            idPedido, idEntrega, idArchivo, authentication);

        return respuesta(
            entregaService.abrirPreview(acceso.archivo()),
            MediaType.parseMediaType("image/webp"),
            "preview-" + acceso.archivo().getIdArchivo() + ".webp",
            false);
    }

    private Acceso cargarAutorizado(
            Long idPedido,
            Long idEntrega,
            Long idArchivo,
            Authentication authentication) {
        EntregaArchivo archivo = entregaService.obtenerArchivo(
            idPedido, idEntrega, idArchivo);
        Pedido pedido = archivo.getEntrega().getHitoPedido().getPedido();

        Long idUsuario = usuarioService
            .getByNombreUsuario(authentication.getName())
            .getIdUsuario();
        boolean esArtista = pedido.getArtista().getIdUsuario().equals(idUsuario);
        boolean esCliente = pedido.getCliente().getIdUsuario().equals(idUsuario);
        boolean esAdmin = esAdmin(authentication);

        if (!esArtista && !esCliente && !esAdmin) {
            throw new AccesoDenegadoException("No podés ver este archivo.");
        }
        if (archivo.isRetirado() && !esAdmin) {
            throw new NoSuchElementException("Archivo no disponible.");
        }

        return new Acceso(archivo, esArtista, esAdmin);
    }

    private ResponseEntity<Resource> respuesta(
            Resource recurso,
            MediaType mediaType,
            String nombre,
            boolean descarga) {
        ContentDisposition.Builder disposition = descarga
            ? ContentDisposition.attachment()
            : ContentDisposition.inline();
        String cabecera = disposition
            .filename(nombre, StandardCharsets.UTF_8)
            .build()
            .toString();

        return ResponseEntity.ok()
            .contentType(mediaType)
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, cabecera)
            .header("X-Content-Type-Options", "nosniff")
            .body(recurso);
    }

    private MediaType mediaType(String mime) {
        try {
            return MediaType.parseMediaType(mime);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(autoridad -> "ROLE_ADMIN".equals(autoridad.getAuthority()));
    }

    private record Acceso(
            EntregaArchivo archivo, boolean esArtista, boolean esAdmin) {
    }
}
