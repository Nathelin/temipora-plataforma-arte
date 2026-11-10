package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.temipora.backend_springboot.modelo.ImagenProductoSubidaDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.ProductoDTO;
import com.temipora.backend_springboot.modelo.ProductoLedgerDTO;
import com.temipora.backend_springboot.modelo.ProductoRequest;
import com.temipora.backend_springboot.modelo.ProductoResumenEstadoDTO;
import com.temipora.backend_springboot.modelo.ProductoVarianteDTO;
import com.temipora.backend_springboot.servicios.AlmacenPrivadoService;
import com.temipora.backend_springboot.servicios.LedgerService;
import com.temipora.backend_springboot.servicios.ProductoBajaService;
import com.temipora.backend_springboot.servicios.ProductoService;
import com.temipora.backend_springboot.servicios.StorageService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ProductoController {

    private final ProductoService productoService;
    private final ProductoBajaService productoBajaService;
    private final LedgerService ledgerService;
    private final UsuarioService usuarioService;
    private final StorageService storageService;
    private final AlmacenPrivadoService almacenPrivadoService;

    public ProductoController(
            ProductoService productoService,
            ProductoBajaService productoBajaService,
            LedgerService ledgerService,
            UsuarioService usuarioService,
            StorageService storageService,
            AlmacenPrivadoService almacenPrivadoService) {
        this.productoService = productoService;
        this.productoBajaService = productoBajaService;
        this.ledgerService = ledgerService;
        this.usuarioService = usuarioService;
        this.storageService = storageService;
        this.almacenPrivadoService = almacenPrivadoService;
    }

    @GetMapping("/mis-productos")
    @PreAuthorize("hasRole('ARTISTA')")
    public List<ProductoDTO> misProductos(Authentication authentication) {
        return productoService.listarMios(idAutenticado(authentication));
    }

    @GetMapping("/mis-productos/{id}")
    @PreAuthorize("hasRole('ARTISTA')")
    public ProductoDTO obtener(
            @PathVariable Long id,
            Authentication authentication) {
        return productoService.obtenerPropio(id, idAutenticado(authentication));
    }

    @GetMapping("/mis-productos/{id}/variantes")
    @PreAuthorize("hasRole('ARTISTA')")
    public PaginaDTO<ProductoVarianteDTO> variantes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "4") int tamano,
            Authentication authentication) {
        return productoService.obtenerVariantes(
            id, idAutenticado(authentication), pagina, tamano);
    }

    @PostMapping("/mis-productos")
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<ProductoDTO> crear(
            @Valid @RequestBody ProductoRequest request,
            Authentication authentication) {
        ProductoDTO creado = productoService.crear(
            idAutenticado(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/mis-productos/{id}")
    @PreAuthorize("hasRole('ARTISTA')")
    public ProductoDTO modificar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request,
            Authentication authentication) {
        return productoService.modificar(
            id, idAutenticado(authentication), request);
    }

    @PostMapping("/mis-productos/{id}/pausar")
    @PreAuthorize("hasRole('ARTISTA')")
    public ProductoDTO pausar(
            @PathVariable Long id,
            Authentication authentication) {
        return productoService.pausar(id, idAutenticado(authentication));
    }

    @PostMapping("/mis-productos/{id}/reactivar")
    @PreAuthorize("hasRole('ARTISTA')")
    public ProductoDTO reactivar(
            @PathVariable Long id,
            Authentication authentication) {
        return productoService.reactivar(id, idAutenticado(authentication));
    }

    @PostMapping("/mis-productos/{id}/archivar")
    @PreAuthorize("hasRole('ARTISTA')")
    public ProductoDTO archivar(
            @PathVariable Long id,
            Authentication authentication) {
        return productoBajaService.archivar(id, idAutenticado(authentication));
    }

    @GetMapping("/mis-productos/{id}/resumen")
    @PreAuthorize("hasRole('ARTISTA')")
    public ProductoResumenEstadoDTO resumen(
            @PathVariable Long id,
            Authentication authentication) {
        return productoBajaService.resumenEstado(
            id, idAutenticado(authentication));
    }

    @GetMapping("/mis-productos/{id}/ledger")
    @PreAuthorize("hasRole('ARTISTA')")
    public ProductoLedgerDTO ledger(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "4") int tamano,
            Authentication authentication) {
        return ledgerService.verificarProducto(
            productoService.obtenerEntidadPropia(
                id, idAutenticado(authentication)),
            pagina,
            tamano);
    }

    @PostMapping("/mis-productos/imagenes")
    @PreAuthorize("hasRole('ARTISTA')")
    public ImagenProductoSubidaDTO subirImagen(
            @RequestPart("archivo") MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Seleccioná una imagen.");
        }
        String url = storageService.guardar(archivo);
        AlmacenPrivadoService.ArchivoGuardado original =
            almacenPrivadoService.guardarOriginal(archivo, "productos");
        return new ImagenProductoSubidaDTO(
            url,
            original.clave(),
            original.sha256(),
            original.nombreOriginal(),
            original.mime(),
            original.ancho(),
            original.alto(),
            original.tamanoBytes());
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
