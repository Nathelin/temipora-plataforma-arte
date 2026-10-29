package com.temipora.backend_springboot.servicios;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.FiltroGaleriaDTO;
import com.temipora.backend_springboot.modelo.GaleriaArchivoDTO;
import com.temipora.backend_springboot.modelo.GaleriaPedidosDTO;
import com.temipora.backend_springboot.modelo.GaleriaPiezaDTO;
import com.temipora.backend_springboot.modelo.ItemSolicitud;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.ReseniaPublicaResumenDTO;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioResumenDTO;
import com.temipora.backend_springboot.repositorios.EntregaArchivoRepository;
import com.temipora.backend_springboot.repositorios.GaleriaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class GaleriaPublicaService {

    private static final int TAMANIO_MAXIMO = 50;

    private final GaleriaRepository galeriaRepository;
    private final EntregaArchivoRepository archivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final ReseniaPublicaService reseniaPublicaService;

    public GaleriaPublicaService(
            GaleriaRepository galeriaRepository,
            EntregaArchivoRepository archivoRepository,
            UsuarioRepository usuarioRepository,
            UsuarioService usuarioService,
            ReseniaPublicaService reseniaPublicaService) {
        this.galeriaRepository = galeriaRepository;
        this.archivoRepository = archivoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.reseniaPublicaService = reseniaPublicaService;
    }

    @Transactional(readOnly = true)
    public GaleriaPedidosDTO listar(
            String nombreUsuario,
            Long viewerId,
            Long idServicio,
            Long idArtista,
            int pagina,
            int tamanio) {
        Usuario duenio = usuarioService.getByNombreUsuario(nombreUsuario);
        boolean esDuenio = viewerId != null
            && duenio.getIdUsuario().equals(viewerId);
        boolean visible = duenio.isPedidosPublicos();
        if (!esDuenio && !visible) {
            return vacia(pagina, tamanio);
        }

        EPreferenciaContenidoSensible preferencia = viewerId == null
            ? EPreferenciaContenidoSensible.ADVERTIR
            : usuarioRepository.findByIdUsuario(viewerId)
                .map(Usuario::getPreferenciaSensible)
                .orElse(EPreferenciaContenidoSensible.ADVERTIR);
        boolean ocultarSensibles = !esDuenio
            && preferencia == EPreferenciaContenidoSensible.OCULTAR;
        boolean advertir = !esDuenio
            && preferencia == EPreferenciaContenidoSensible.ADVERTIR;

        Pageable pageable = PageRequest.of(
            Math.max(0, pagina),
            Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO));
        Page<Object[]> page = galeriaRepository.findPaginaGaleria(
            duenio.getIdUsuario(),
            EstadoPedido.FINALIZADO,
            esDuenio,
            viewerId,
            ocultarSensibles,
            idServicio,
            idArtista,
            pageable);

        List<ItemSolicitud> items = page.getContent().stream()
            .map(fila -> (ItemSolicitud) fila[0])
            .toList();
        List<Long> ids = items.stream()
            .map(ItemSolicitud::getIdItemSolicitud)
            .toList();
        Map<Long, List<EntregaArchivo>> archivosPorItem = ids.isEmpty()
            ? Map.of()
            : archivoRepository.listarGaleriaPorItems(ids).stream()
                .collect(Collectors.groupingBy(
                    archivo -> archivo.getItemSolicitud()
                        .getIdItemSolicitud(),
                    LinkedHashMap::new,
                    Collectors.toList()));

        List<Long> idsPedido = archivosPorItem.values().stream()
            .flatMap(List::stream)
            .map(archivo -> archivo.getEntrega().getHitoPedido()
                .getPedido().getIdPedido())
            .distinct()
            .toList();
        Map<Long, ReseniaPublicaResumenDTO> reseniasCrudas =
            reseniaPublicaService.resumenPorPedidos(idsPedido);
        final Map<Long, ReseniaPublicaResumenDTO> resenias =
            reseniasCrudas != null ? reseniasCrudas : Map.of();

        List<GaleriaPiezaDTO> piezas = items.stream()
            .map(item -> aPieza(
                item,
                archivosPorItem.getOrDefault(
                    item.getIdItemSolicitud(), List.of()),
                esDuenio,
                advertir,
                resenias))
            .filter(Objects::nonNull)
            .toList();

        List<FiltroGaleriaDTO> servicios = galeriaRepository
            .serviciosDisponibles(
                duenio.getIdUsuario(),
                EstadoPedido.FINALIZADO,
                esDuenio,
                viewerId,
                ocultarSensibles)
            .stream()
            .map(this::aFiltroServicio)
            .toList();
        List<FiltroGaleriaDTO> artistas = galeriaRepository
            .artistasDisponibles(
                duenio.getIdUsuario(),
                EstadoPedido.FINALIZADO,
                esDuenio,
                viewerId,
                ocultarSensibles)
            .stream()
            .map(this::aFiltroArtista)
            .toList();

        return GaleriaPedidosDTO.builder()
            .visible(visible)
            .esDuenio(esDuenio)
            .piezas(aPagina(page, piezas))
            .servicios(servicios)
            .artistas(artistas)
            .build();
    }

    @Transactional(readOnly = true)
    public EntregaArchivo autorizarPreview(
            Long idArchivo, Long viewerId) {
        EntregaArchivo archivo = archivoRepository
            .buscarParaGaleria(idArchivo)
            .orElseThrow(() -> new NoSuchElementException(
                "Archivo no disponible."));
        Pedido pedido = archivo.getEntrega().getHitoPedido().getPedido();

        if (archivo.isRetirado()
                || archivo.getEntrega().isRetirada()
                || !archivo.getEntrega().isVigente()
                || !archivo.getEntrega().isEsFinal()
                || archivo.getMime() == null
                || !archivo.getMime().startsWith("image/")
                || pedido.getEstado() != EstadoPedido.FINALIZADO
                || !pedido.isPublicacionAutorizada()) {
            throw new NoSuchElementException("Archivo no disponible.");
        }

        boolean esCliente = pedido.getCliente().getIdUsuario()
            .equals(viewerId);
        boolean esArtista = pedido.getArtista().getIdUsuario()
            .equals(viewerId);
        boolean esPublica = pedido.getCliente().isPedidosPublicos()
            && !pedido.isPublicacionOcultadaCliente();
        if (!esCliente && !esArtista && !esPublica) {
            throw new NoSuchElementException("Archivo no disponible.");
        }
        return archivo;
    }

    private GaleriaPiezaDTO aPieza(
            ItemSolicitud item,
            List<EntregaArchivo> archivos,
            boolean esDuenio,
            boolean advertir,
            Map<Long, ReseniaPublicaResumenDTO> resenias) {
        if (archivos.isEmpty()) {
            return null;
        }
        EntregaArchivo primero = archivos.get(0);
        Pedido pedido = primero.getEntrega().getHitoPedido().getPedido();
        Servicio servicio = pedido.getServicio();
        boolean sensible = servicio.isContenidoSensible();
        ReseniaPublicaResumenDTO resenia = resenias.get(pedido.getIdPedido());
        ReseniaPublicaResumenDTO reseniaVisible = resenia != null
            && (esDuenio || !resenia.isAnonima())
            ? resenia
            : null;

        return GaleriaPiezaDTO.builder()
            .idPieza(item.getIdItemSolicitud())
            .nombreDibujo(item.getNombre())
            .idServicio(servicio.getIdServicio())
            .nombreServicio(servicio.getNombre())
            .servicioDisponible(servicio.getEstado() == EstadoServicio.PUBLICADO
                && servicio.getModoRecepcion() != ModoRecepcionSolicitud.CERRADA)
            .artista(UsuarioResumenDTO.desdeUsuario(pedido.getArtista()))
            .fechaEntrega(primero.getEntrega().getFechaEntrega())
            .contenidoSensible(sensible)
            .contenidosSensibles(servicio.getContenidosSensibles().stream()
                .map(Enum::name)
                .toList())
            .requiereAdvertencia(advertir && sensible)
            .archivos(archivos.stream()
                .map(archivo -> GaleriaArchivoDTO.builder()
                    .idArchivo(archivo.getIdArchivo())
                    .urlPreview("/galeria/archivos/"
                        + archivo.getIdArchivo() + "/preview")
                    .ancho(archivo.getAncho())
                    .alto(archivo.getAlto())
                    .orden(archivo.getOrden())
                    .build())
                .toList())
            .idPedido(esDuenio ? pedido.getIdPedido() : null)
            .ocultoPorCliente(esDuenio
                ? pedido.isPublicacionOcultadaCliente()
                : null)
            .resenia(reseniaVisible)
            .build();
    }

    private FiltroGaleriaDTO aFiltroServicio(Servicio servicio) {
        return FiltroGaleriaDTO.builder()
            .id(servicio.getIdServicio())
            .nombre(servicio.getNombre())
            .build();
    }

    private FiltroGaleriaDTO aFiltroArtista(Usuario artista) {
        String nombre = artista.getNombreDisplay() != null
                && !artista.getNombreDisplay().isBlank()
            ? artista.getNombreDisplay()
            : artista.getNombreUsuario();
        return FiltroGaleriaDTO.builder()
            .id(artista.getIdUsuario())
            .nombre(nombre)
            .nombreUsuario(artista.getNombreUsuario())
            .build();
    }

    private PaginaDTO<GaleriaPiezaDTO> aPagina(
            Page<?> page,
            List<GaleriaPiezaDTO> contenido) {
        return PaginaDTO.<GaleriaPiezaDTO>builder()
            .contenido(contenido)
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    private GaleriaPedidosDTO vacia(int pagina, int tamanio) {
        return GaleriaPedidosDTO.builder()
            .visible(false)
            .esDuenio(false)
            .piezas(PaginaDTO.<GaleriaPiezaDTO>builder()
                .contenido(List.of())
                .pagina(Math.max(0, pagina))
                .tamanio(Math.max(1, tamanio))
                .totalElementos(0)
                .totalPaginas(0)
                .hayMas(false)
                .build())
            .servicios(List.of())
            .artistas(List.of())
            .build();
    }
}
