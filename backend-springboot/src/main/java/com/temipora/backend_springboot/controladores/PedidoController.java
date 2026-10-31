package com.temipora.backend_springboot.controladores;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import com.temipora.backend_springboot.modelo.ContadoresPedidosDTO;
import com.temipora.backend_springboot.modelo.EntregaHitoMultipartRequest;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.PedidoDTO;
import com.temipora.backend_springboot.modelo.PedidoResumenDTO;
import com.temipora.backend_springboot.modelo.PublicacionPedidoRequest;
import com.temipora.backend_springboot.modelo.SolicitudRevisionRequest;
import com.temipora.backend_springboot.modelo.TipoDescarga;
import com.temipora.backend_springboot.servicios.ConstanciaService;
import com.temipora.backend_springboot.servicios.DescargaService;
import com.temipora.backend_springboot.servicios.PedidoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;
    private final ConstanciaService constanciaService;
    private final DescargaService descargaService;

    public PedidoController(
            PedidoService pedidoService,
            UsuarioService usuarioService,
            ConstanciaService constanciaService,
            DescargaService descargaService) {
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
        this.constanciaService = constanciaService;
        this.descargaService = descargaService;
    }

    @GetMapping("/{idPedido}")
    public PedidoDTO obtener(
            @PathVariable Long idPedido,
            Authentication authentication) {
        return pedidoService.obtener(
            idPedido,
            idAutenticado(authentication),
            esAdmin(authentication));
    }

    @GetMapping("/{idPedido}/constancia")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA', 'ADMIN')")
    public ResponseEntity<byte[]> constancia(
            @PathVariable Long idPedido,
            Authentication authentication,
            HttpServletRequest request) {
        Long idUsuario = idAutenticado(authentication);
        boolean esAdmin = esAdmin(authentication);
        PedidoDTO pedido = pedidoService.obtener(
            idPedido, idUsuario, esAdmin);
        byte[] pdf = constanciaService.generar(pedido);

        descargaService.registrar(
            idPedido,
            idUsuario,
            null,
            TipoDescarga.CONSTANCIA,
            request.getRemoteAddr());

        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(
                "constancia-pedido-" + idPedido + ".pdf",
                StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(pdf);
    }

    @GetMapping("/recibidos")
    @PreAuthorize("hasRole('ARTISTA')")
    public PaginaDTO<PedidoResumenDTO> recibidos(
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) Long idServicio,
            @RequestParam(required = false, name = "q") String busqueda,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            Authentication authentication) {
        return pedidoService.listarRecibidos(
            idAutenticado(authentication),
            new PedidoService.FiltroPedidos(
                estado, grupo, idServicio, busqueda, orden),
            pagina,
            tamanio);
    }

    @GetMapping("/enviados")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PaginaDTO<PedidoResumenDTO> enviados(
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) Long idServicio,
            @RequestParam(required = false, name = "q") String busqueda,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            Authentication authentication) {
        return pedidoService.listarEnviados(
            idAutenticado(authentication),
            new PedidoService.FiltroPedidos(
                estado, grupo, idServicio, busqueda, orden),
            pagina,
            tamanio);
    }

    @GetMapping("/contadores")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public ContadoresPedidosDTO contadores(
            @RequestParam(defaultValue = "recibidos") String bandeja,
            Authentication authentication) {
        return pedidoService.contadores(
            idAutenticado(authentication), "recibidos".equals(bandeja));
    }

    @PostMapping("/{idPedido}/hitos/{idHito}/pagar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO pagarHito(
            @PathVariable Long idPedido,
            @PathVariable Long idHito,
            Authentication authentication) {
        return pedidoService.pagarHito(
            idPedido, idHito, idAutenticado(authentication));
    }

    @PostMapping("/{idPedido}/hitos/{idHito}/aprobar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO aprobar(
            @PathVariable Long idPedido,
            @PathVariable Long idHito,
            Authentication authentication) {
        return pedidoService.aprobar(
            idPedido, idHito, idAutenticado(authentication));
    }

    @PostMapping("/{idPedido}/hitos/{idHito}/solicitar-revision")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO solicitarRevision(
            @PathVariable Long idPedido,
            @PathVariable Long idHito,
            @RequestBody(required = false) SolicitudRevisionRequest request,
            Authentication authentication) {
        return pedidoService.solicitarRevision(
            idPedido, idHito, idAutenticado(authentication), request);
    }

    @PostMapping(
        value = "/{idPedido}/hitos/{idHito}/entregas",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ARTISTA')")
    public PedidoDTO entregar(
            @PathVariable Long idPedido,
            @PathVariable Long idHito,
            @RequestPart("datos") @Valid EntregaHitoMultipartRequest datos,
            @RequestPart("archivos") List<MultipartFile> archivos,
            Authentication authentication) {
        return pedidoService.entregar(
            idPedido, idHito, idAutenticado(authentication), datos, archivos);
    }

    @PostMapping("/{idPedido}/cancelar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO cancelar(
            @PathVariable Long idPedido,
            Authentication authentication) {
        return pedidoService.cancelar(
            idPedido, idAutenticado(authentication));
    }

    @PutMapping("/{idPedido}/publicacion/artista")
    @PreAuthorize("hasRole('ARTISTA')")
    public PedidoDTO autorizarPublicacion(
            @PathVariable Long idPedido,
            @RequestBody PublicacionPedidoRequest request,
            Authentication authentication) {
        return pedidoService.autorizarPublicacion(
            idPedido,
            idAutenticado(authentication),
            request.isAutoriza());
    }

    @PutMapping("/{idPedido}/publicacion/cliente")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO ocultarPublicacion(
            @PathVariable Long idPedido,
            @RequestBody PublicacionPedidoRequest request,
            Authentication authentication) {
        return pedidoService.ocultarPublicacion(
            idPedido,
            idAutenticado(authentication),
            request.isOcultar());
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(autoridad -> "ROLE_ADMIN".equals(autoridad.getAuthority()));
    }
}
