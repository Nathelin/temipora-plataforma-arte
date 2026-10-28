package com.temipora.backend_springboot.servicios;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.temipora.backend_springboot.modelo.ArchivoEntregaRequest;
import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.EntregaHito;
import com.temipora.backend_springboot.modelo.EntregaHitoMultipartRequest;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.ItemSolicitud;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EntregaArchivoRepository;
import com.temipora.backend_springboot.repositorios.EntregaHitoRepository;

@Service
public class EntregaService {

    static final int MAX_ARCHIVOS = 20;

    private final EntregaHitoRepository entregaRepository;
    private final EntregaArchivoRepository archivoRepository;
    private final AlmacenPrivadoService almacen;
    private final PreviewService previewService;

    public EntregaService(
            EntregaHitoRepository entregaRepository,
            EntregaArchivoRepository archivoRepository,
            AlmacenPrivadoService almacen,
            PreviewService previewService) {
        this.entregaRepository = entregaRepository;
        this.archivoRepository = archivoRepository;
        this.almacen = almacen;
        this.previewService = previewService;
    }

    @Transactional
    public EntregaHito crear(
            Pedido pedido,
            HitoPedido hito,
            EntregaHitoMultipartRequest request,
            List<MultipartFile> archivos) {
        validar(pedido, request, archivos);

        entregaRepository
            .findFirstByHitoPedidoIdHitoPedidoAndVigenteTrue(hito.getIdHitoPedido())
            .ifPresent(anterior -> {
                anterior.setVigente(false);
                entregaRepository.save(anterior);
            });

        int numero = entregaRepository
            .findFirstByHitoPedidoIdHitoPedidoOrderByNumeroDesc(
                hito.getIdHitoPedido())
            .map(anterior -> anterior.getNumero() + 1)
            .orElse(1);

        String marca = "Pedido #" + pedido.getIdPedido()
            + " · " + LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            + " · v" + numero;

        EntregaHito entrega = new EntregaHito();
        entrega.setHitoPedido(hito);
        entrega.setNumero(numero);
        entrega.setAutor(pedido.getArtista());
        entrega.setDescripcion(limpiar(request.getDescripcion(), 500));
        entrega.setFechaEntrega(LocalDateTime.now());
        entrega.setVigente(true);

        List<String> clavesGuardadas = new ArrayList<>();
        try {
            for (int i = 0; i < archivos.size(); i++) {
                MultipartFile archivo = archivos.get(i);
                ArchivoEntregaRequest meta = request.getArchivos().get(i);
                ItemSolicitud dibujo = resolverDibujo(
                    pedido.getSolicitud().getItems(), meta.getIdItemSolicitud());

                String subcarpeta = "pedidos/" + pedido.getIdPedido()
                    + "/hitos/" + hito.getIdHitoPedido()
                    + "/entregas/" + numero;
                AlmacenPrivadoService.ArchivoGuardado guardado =
                    almacen.guardarOriginal(archivo, subcarpeta);
                clavesGuardadas.add(guardado.clave());

                byte[] preview = previewService.generar(
                    almacen.leer(guardado.clave()), marca);
                String clavePreview = almacen.guardarPreview(
                    preview, subcarpeta);
                clavesGuardadas.add(clavePreview);

                EntregaArchivo entidad = new EntregaArchivo();
                entidad.setItemSolicitud(dibujo);
                entidad.setNombreOriginal(nombreVisible(meta, guardado));
                entidad.setClaveOriginal(guardado.clave());
                entidad.setClavePreview(clavePreview);
                entidad.setSha256(guardado.sha256());
                entidad.setMime(guardado.mime());
                entidad.setAncho(guardado.ancho());
                entidad.setAlto(guardado.alto());
                entidad.setTamanoBytes(guardado.tamanoBytes());
                entidad.setOrden(i);
                entrega.agregarArchivo(entidad);
            }

            return entregaRepository.save(entrega);
        } catch (RuntimeException e) {
            clavesGuardadas.forEach(almacen::eliminar);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<EntregaConArchivos> listarPorPedido(Long idPedido) {
        List<EntregaHito> entregas = entregaRepository.listarPorPedido(idPedido);
        if (entregas.isEmpty()) {
            return List.of();
        }

        List<Long> ids = entregas.stream()
            .map(EntregaHito::getIdEntrega)
            .toList();
        Map<Long, List<EntregaArchivo>> archivosPorEntrega =
            archivoRepository.listarPorEntregas(ids).stream()
                .collect(Collectors.groupingBy(
                    archivo -> archivo.getEntrega().getIdEntrega(),
                    LinkedHashMap::new,
                    Collectors.toList()));

        return entregas.stream()
            .map(entrega -> new EntregaConArchivos(
                entrega,
                archivosPorEntrega.getOrDefault(
                    entrega.getIdEntrega(), List.of())))
            .toList();
    }

    @Transactional
    public void registrarRevision(HitoPedido hito, Usuario autor, String motivo) {
        entregaRepository
            .findFirstByHitoPedidoIdHitoPedidoAndVigenteTrue(hito.getIdHitoPedido())
            .ifPresent(entrega -> {
                entrega.setRevisionMotivo(limpiar(motivo, 500));
                entrega.setRevisionFecha(LocalDateTime.now());
                entrega.setRevisionPor(autor);
                entregaRepository.save(entrega);
            });
    }

    @Transactional(readOnly = true)
    public EntregaArchivo obtenerArchivo(
            Long idPedido, Long idEntrega, Long idArchivo) {
        return archivoRepository
            .buscarDelPedido(idArchivo, idEntrega, idPedido)
            .orElseThrow(() -> new NoSuchElementException("Archivo no encontrado."));
    }

    public Resource abrir(EntregaArchivo archivo) {
        return almacen.abrir(archivo.getClaveOriginal());
    }

    public Resource abrirPreview(EntregaArchivo archivo) {
        if (archivo.getClavePreview() == null) {
            throw new NoSuchElementException("Preview no disponible.");
        }
        return almacen.abrir(archivo.getClavePreview());
    }

    @Transactional(readOnly = true)
    public EntregaConArchivos obtenerEntrega(
            Long idPedido, Long idEntrega) {
        EntregaHito entrega = entregaRepository
            .buscarDelPedido(idEntrega, idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Entrega no encontrada."));
        List<EntregaArchivo> archivos = archivoRepository
            .listarPorEntregas(List.of(idEntrega));
        return new EntregaConArchivos(entrega, archivos);
    }

    public void escribirZip(
            EntregaConArchivos entrega, OutputStream salida)
            throws IOException {
        List<ArchivoZip> archivos = new ArrayList<>();
        Set<String> usados = new HashSet<>();
        for (EntregaArchivo archivo : entrega.archivos()) {
            if (archivo.isRetirado()) {
                continue;
            }
            archivos.add(new ArchivoZip(
                nombreZip(archivo, usados), archivo));
        }

        try (ZipOutputStream zip = new ZipOutputStream(
                salida, StandardCharsets.UTF_8)) {
            for (ArchivoZip item : archivos) {
                zip.putNextEntry(new ZipEntry(item.nombre()));
                try (InputStream entrada = almacen
                        .abrir(item.archivo().getClaveOriginal())
                        .getInputStream()) {
                    entrada.transferTo(zip);
                }
                zip.closeEntry();
            }

            zip.putNextEntry(new ZipEntry("manifiesto.txt"));
            zip.write(manifiesto(entrega, archivos)
                .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.finish();
        }
    }

    public boolean puedeVerOriginal(
            EntregaArchivo archivo, boolean esArtista, boolean esAdmin) {
        return puedeVerOriginal(
            archivo.getEntrega().getHitoPedido(), esArtista, esAdmin);
    }

    public boolean puedeVerOriginal(
            HitoPedido hito, boolean esArtista, boolean esAdmin) {
        return esArtista || esAdmin
            || hito.getEstadoPago() == EstadoPagoHito.PAGADO;
    }

    private String nombreZip(
            EntregaArchivo archivo, Set<String> usados) {
        String base = archivo.getItemSolicitud() != null
            ? "Dibujo "
                + ((archivo.getItemSolicitud().getOrden() == null
                    ? 0 : archivo.getItemSolicitud().getOrden()) + 1)
                + " - " + limpiarNombre(archivo.getItemSolicitud().getNombre())
            : "General";

        String nombre = limpiarNombre(archivo.getNombreOriginal());
        if (nombre.isBlank()) {
            nombre = "archivo-" + archivo.getIdArchivo();
        }
        if (nombre.length() > 200) {
            nombre = nombre.substring(0, 200);
        }

        String candidato = base + " - " + nombre;
        int sufijo = 2;
        while (!usados.add(candidato)) {
            candidato = base + " - " + sufijo + " - " + nombre;
            sufijo++;
        }
        return candidato;
    }

    private String limpiarNombre(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll("[\\\\/\\r\\n\\t]", "_").trim();
    }

    private String manifiesto(
            EntregaConArchivos entrega, List<ArchivoZip> archivos) {
        Pedido pedido = entrega.entrega().getHitoPedido().getPedido();
        StringBuilder texto = new StringBuilder();
        texto.append("Pedido #").append(pedido.getIdPedido()).append('\n');
        texto.append("Entrega v").append(entrega.entrega().getNumero())
            .append(" - ").append(entrega.entrega().getFechaEntrega())
            .append('\n');
        texto.append("Artista: @")
            .append(pedido.getArtista().getNombreUsuario()).append('\n');
        texto.append("Cliente: @")
            .append(pedido.getCliente().getNombreUsuario()).append('\n');
        texto.append("Archivos:\n");
        for (ArchivoZip item : archivos) {
            texto.append("- ").append(item.nombre())
                .append(" | sha256=").append(item.archivo().getSha256())
                .append('\n');
        }
        return texto.toString();
    }

    private void validar(
            Pedido pedido,
            EntregaHitoMultipartRequest request,
            List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            throw new IllegalArgumentException(
                "Tenés que subir al menos un archivo.");
        }
        if (request.getArchivos() == null
                || request.getArchivos().size() != archivos.size()) {
            throw new IllegalArgumentException(
                "La cantidad de archivos no coincide con los datos enviados.");
        }
        if (archivos.size() > MAX_ARCHIVOS) {
            throw new IllegalArgumentException(
                "Podés subir hasta " + MAX_ARCHIVOS + " archivos por entrega.");
        }
        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                throw new IllegalArgumentException(
                    "Uno de los archivos está vacío.");
            }
        }
        for (ArchivoEntregaRequest meta : request.getArchivos()) {
            resolverDibujo(pedido.getSolicitud().getItems(), meta.getIdItemSolicitud());
        }
    }

    private ItemSolicitud resolverDibujo(
            List<ItemSolicitud> dibujos, Long idItemSolicitud) {
        if (idItemSolicitud == null) {
            return null;
        }
        return dibujos.stream()
            .filter(item -> idItemSolicitud.equals(item.getIdItemSolicitud()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "El dibujo indicado no pertenece a este pedido."));
    }

    private String nombreVisible(
            ArchivoEntregaRequest meta,
            AlmacenPrivadoService.ArchivoGuardado guardado) {
        String nombre = limpiar(meta.getNombreVisible(), 255);
        return nombre != null ? nombre : guardado.nombreOriginal();
    }

    private String limpiar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }

    public record EntregaConArchivos(
            EntregaHito entrega,
            List<EntregaArchivo> archivos) {
    }

    private record ArchivoZip(
            String nombre,
            EntregaArchivo archivo) {
    }
}
