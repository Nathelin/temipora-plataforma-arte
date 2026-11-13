package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.AnclajeLedger;
import com.temipora.backend_springboot.modelo.CertificadoDTO;
import com.temipora.backend_springboot.modelo.EstadoAnclaje;
import com.temipora.backend_springboot.modelo.EventoLedgerDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.VarianteLedgerDTO;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;

/**
 * Certificado público de autenticidad de una pieza única: reúne la verificación
 * del ledger interno con el estado del anclaje en Bitcoin.
 */
@Service
public class CertificadoService {

    private final ProductoVarianteRepository varianteRepository;
    private final LedgerService ledgerService;
    private final AnclajeService anclajeService;
    private final String urlFrontend;

    public CertificadoService(
            ProductoVarianteRepository varianteRepository,
            LedgerService ledgerService,
            AnclajeService anclajeService,
            @Value("${app.frontend.url:http://localhost:5173}") String urlFrontend) {
        this.varianteRepository = varianteRepository;
        this.ledgerService = ledgerService;
        this.anclajeService = anclajeService;
        this.urlFrontend = urlFrontend != null
            ? urlFrontend.replaceAll("/+$", "")
            : "";
    }

    @Transactional(readOnly = true)
    public CertificadoDTO obtener(String codigo) {
        Long idVariante = CodigoCertificado.decodificar(codigo)
            .orElseThrow(() -> new NoSuchElementException(
                "Certificado no encontrado."));
        ProductoVariante variante = varianteRepository.findById(idVariante)
            .orElseThrow(() -> new NoSuchElementException(
                "Certificado no encontrado."));
        VarianteLedgerDTO ledger = ledgerService.verificarVariante(variante);
        if (!ledger.isTieneLedger()) {
            throw new NoSuchElementException(
                "Esta pieza no tiene certificado de autenticidad.");
        }
        Producto producto = variante.getProducto();
        Usuario artista = producto != null ? producto.getArtista() : null;
        Usuario propietario = variante.getPropietarioActual();

        CertificadoDTO dto = CertificadoDTO.builder()
            .idProducto(producto != null ? producto.getIdProducto() : null)
            .titulo(producto != null ? producto.getTitulo() : null)
            .idVariante(variante.getIdVariante())
            .nombreVariante(variante.getNombre())
            .imagenUrl(variante.getImagenUrl() != null
                ? variante.getImagenUrl()
                : (producto != null ? producto.getImagenPortada() : null))
            .idArtista(artista != null ? artista.getIdUsuario() : null)
            .nombreArtista(artista != null ? artista.getNombreUsuario() : null)
            .nombreDisplayArtista(artista != null ? artista.getNombreDisplay() : null)
            .urlFotoArtista(artista != null
                ? (artista.getUrlFotoPerfilMiniatura() != null
                    ? artista.getUrlFotoPerfilMiniatura()
                    : artista.getUrlFotoPerfil())
                : null)
            .nombrePropietarioActual(propietario != null
                    && propietario.isMostrarPropietario()
                ? propietario.getNombreUsuario() : null)
            .piezaUnica(true)
            .integridad(ledger.isIntegridad())
            .mensaje(ledger.getMensaje())
            .codigoCertificado(CodigoCertificado.codificar(variante.getIdVariante()))
            .urlVerificacion(urlFrontend + "/certificado/"
                + CodigoCertificado.codificar(variante.getIdVariante()))
            .build();

        List<EventoLedgerDTO> eventos = ledger.getEventos();
        dto.setTrayectoria(eventos.stream()
            .map(e -> new CertificadoDTO.TrazaEvento(
                e.getSecuencia(), e.getTipo(), e.getOcurridoEn(), e.isVerificado()))
            .toList());

        aplicarAnclaje(dto, eventos);
        if (!eventos.isEmpty()) {
            anclajeService.pruebaInclusionDeEvento(eventos.get(0).getIdEventoLedger())
                .ifPresent(dto::setPruebaInclusion);
        }
        return dto;
    }

    private void aplicarAnclaje(CertificadoDTO dto, List<EventoLedgerDTO> eventos) {
        if (eventos.isEmpty()) {
            return;
        }
        Collection<Long> ids = new ArrayList<>(eventos.stream()
            .map(EventoLedgerDTO::getIdEventoLedger)
            .toList());
        Map<Long, AnclajeLedger> anclajes = anclajeService.anclajesDeEventos(ids);
        if (anclajes.isEmpty()) {
            dto.setEstadoAnclaje(null);
            return;
        }
        AnclajeLedger mejor = null;
        for (AnclajeLedger anclaje : anclajes.values()) {
            if (mejor == null || puntaje(anclaje) > puntaje(mejor)) {
                mejor = anclaje;
            }
        }
        if (mejor == null) {
            return;
        }
        dto.setEstadoAnclaje(mejor.getEstado());
        dto.setHashRaiz(mejor.getHashRaiz());
        dto.setHashBitcoin(mejor.getHashBitcoin());
        dto.setBitcoinAltura(mejor.getBitcoinAltura());
        LocalDateTime fecha = mejor.getFechaConfirmacion() != null
            ? mejor.getFechaConfirmacion()
            : mejor.getFechaSolicitud();
        dto.setFechaAnclaje(fecha);
    }

    private int puntaje(AnclajeLedger anclaje) {
        return switch (anclaje.getEstado()) {
            case CONFIRMADO -> 2;
            case PENDIENTE -> 1;
            case FALLIDO -> 0;
        };
    }

    public Optional<CertificadoDTO> obtenerOpcional(String codigo) {
        try {
            return Optional.of(obtener(codigo));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }
}
