package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoDTO;
import com.temipora.backend_springboot.modelo.ProductoResumenEstadoDTO;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class ProductoBajaService {

    private static final String ENTIDAD = "Producto";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("estado", "Estado"));

    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EventoLedgerRepository eventoLedgerRepository;
    private final ProductoService productoService;
    private final AuditoriaService auditoria;

    public ProductoBajaService(
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository,
            EventoLedgerRepository eventoLedgerRepository,
            ProductoService productoService,
            AuditoriaService auditoria) {
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventoLedgerRepository = eventoLedgerRepository;
        this.productoService = productoService;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public ProductoResumenEstadoDTO resumenEstado(Long idProducto, Long idArtista) {
        Producto producto = productoRepository.findByIdProducto(idProducto)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto no encontrado: " + idProducto));
        productoService.verificarDuenio(producto, idArtista);

        long disponibles = producto.getVariantes().stream()
            .filter(ProductoVariante::estaDisponible)
            .count();
        long agotadas = producto.getVariantes().stream()
            .filter(variante -> variante.getEstado() == EstadoVariante.AGOTADO
                || (variante.getStockDisponible() != null
                    && variante.getStockDisponible() == 0))
            .count();
        long archivadas = producto.getVariantes().stream()
            .filter(variante -> variante.getEstado() == EstadoVariante.ARCHIVADO)
            .count();
        long conLedger = eventoLedgerRepository
            .findVariantesConLedger(List.of(idProducto))
            .size();

        return ProductoResumenEstadoDTO.builder()
            .idProducto(idProducto)
            .totalVariantes(producto.getVariantes().size())
            .variantesDisponibles(disponibles)
            .variantesAgotadas(agotadas)
            .variantesArchivadas(archivadas)
            .variantesConLedger(conLedger)
            .build();
    }

    @Transactional
    public ProductoDTO archivar(Long idProducto, Long idArtista) {
        Producto producto = productoRepository.findByIdProductoForUpdate(idProducto)
            .orElseThrow(() -> new NoSuchElementException(
                "Producto no encontrado: " + idProducto));
        productoService.verificarDuenio(producto, idArtista);

        if (producto.getEstado() == EstadoProducto.ARCHIVADO) {
            throw new ConflictoException("El producto ya está archivado.");
        }

        Snapshot antes = Snapshot.de(producto, CAMPOS);
        producto.setEstado(EstadoProducto.ARCHIVADO);
        producto.setArchivadoEn(LocalDateTime.now());
        producto.setArchivadoPor(usuarioRepository.findByIdUsuario(idArtista)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idArtista)));
        producto.setFechaActualizacion(LocalDateTime.now());
        productoRepository.save(producto);

        auditoria.registrar(
            AccionesAdmin.PRODUCTO_ARCHIVAR, ENTIDAD, producto.getIdProducto(),
            "Dio de baja el producto “" + producto.getTitulo() + "”",
            antes, producto, CAMPOS);
        return productoService.aDTO(producto);
    }
}
