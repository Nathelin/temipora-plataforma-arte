package com.temipora.backend_springboot.servicios;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.AnclajeDTO;
import com.temipora.backend_springboot.modelo.AnclajeLedger;
import com.temipora.backend_springboot.modelo.AnclajeLedgerEvento;
import com.temipora.backend_springboot.modelo.EstadoAnclaje;
import com.temipora.backend_springboot.modelo.EventoLedger;
import com.temipora.backend_springboot.repositorios.AnclajeLedgerEventoRepository;
import com.temipora.backend_springboot.repositorios.AnclajeLedgerRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;

/**
 * Anclaje del registro de autenticidad en Bitcoin.
 *
 * Toma los eventos nuevos del ledger, calcula su raíz de Merkle y la sella con
 * OpenTimestamps. Guarda la prueba y, cuando el lote es minado, la altura del
 * bloque de Bitcoin. Nunca modifica {@code evento_ledger}.
 */
@Service
public class AnclajeService {

    private static final Logger log = LoggerFactory.getLogger(AnclajeService.class);

    private final EventoLedgerRepository eventoRepository;
    private final AnclajeLedgerRepository anclajeRepository;
    private final AnclajeLedgerEventoRepository anclajeEventoRepository;
    private final MerkleService merkleService;
    private final ProveedorSellado proveedorSellado;
    private final AuditoriaService auditoria;

    private final int tamanoLote;

    public AnclajeService(
            EventoLedgerRepository eventoRepository,
            AnclajeLedgerRepository anclajeRepository,
            AnclajeLedgerEventoRepository anclajeEventoRepository,
            MerkleService merkleService,
            ProveedorSellado proveedorSellado,
            AuditoriaService auditoria,
            @Value("${temipora.anclaje.tamano-lote:500}") int tamanoLote) {
        this.eventoRepository = eventoRepository;
        this.anclajeRepository = anclajeRepository;
        this.anclajeEventoRepository = anclajeEventoRepository;
        this.merkleService = merkleService;
        this.proveedorSellado = proveedorSellado;
        this.auditoria = auditoria;
        this.tamanoLote = Math.max(1, tamanoLote);
    }

    /**
     * Sella en Bitcoin los eventos posteriores al último anclaje. Devuelve el
     * anclaje creado, o vacío si no hay eventos nuevos.
     */
    @Transactional
    public Optional<AnclajeLedger> anclar() {
        long desde = anclajeRepository.findTopByOrderByIdAnclajeDesc()
            .map(AnclajeLedger::getIdEventoHasta)
            .filter(java.util.Objects::nonNull)
            .orElse(0L);

        List<EventoLedger> eventos = eventoRepository
            .findByIdEventoLedgerGreaterThanOrderByIdEventoLedgerAsc(
                desde, PageRequest.of(0, tamanoLote));
        if (eventos.isEmpty()) {
            return Optional.empty();
        }

        List<String> hashes = eventos.stream()
            .map(EventoLedger::getHashEvento)
            .toList();
        String raiz = merkleService.raiz(hashes);
        byte[] digest = sha256(raiz.getBytes(StandardCharsets.UTF_8));

        ProveedorSellado.ResultadoSellado resultado = proveedorSellado.sellar(digest);

        AnclajeLedger anclaje = new AnclajeLedger();
        anclaje.setHashRaiz(raiz);
        anclaje.setCantidadEventos(eventos.size());
        anclaje.setIdEventoDesde(eventos.get(0).getIdEventoLedger());
        anclaje.setIdEventoHasta(eventos.get(eventos.size() - 1).getIdEventoLedger());
        anclaje.setEstado(EstadoAnclaje.PENDIENTE);
        anclaje.setPruebaOts(resultado.pruebaOts());
        anclaje.setCalendarioUrl(recortar(resultado.calendarios(), 255));
        anclaje.setFechaSolicitud(LocalDateTime.now());
        AnclajeLedger guardado = anclajeRepository.save(anclaje);

        int posicion = 0;
        for (EventoLedger evento : eventos) {
            AnclajeLedgerEvento link = new AnclajeLedgerEvento();
            link.setIdAnclaje(guardado.getIdAnclaje());
            link.setIdEventoLedger(evento.getIdEventoLedger());
            link.setPosicion(posicion++);
            anclajeEventoRepository.save(link);
        }

        auditoria.registrarSistema(AccionesAdmin.LEDGER_ANCLAR, "AnclajeLedger",
            guardado.getIdAnclaje(),
            "Selló " + eventos.size() + " evento(s) del ledger (raíz " + raiz + ")");
        log.info("Anclaje {} selló {} evento(s) del ledger.", guardado.getIdAnclaje(),
            eventos.size());
        return Optional.of(guardado);
    }

    /**
     * Intenta confirmar los anclajes pendientes contra Bitcoin. Devuelve
     * cuántos pasaron a CONFIRMADO.
     */
    @Transactional
    public int confirmarPendientes() {
        List<AnclajeLedger> pendientes =
            anclajeRepository.findByEstadoOrderByIdAnclajeAsc(EstadoAnclaje.PENDIENTE);
        int confirmados = 0;
        for (AnclajeLedger anclaje : pendientes) {
            if (anclaje.getPruebaOts() == null) {
                continue;
            }
            Optional<ProveedorSellado.ResultadoConfirmacion> resultado;
            try {
                resultado = proveedorSellado.confirmar(anclaje.getPruebaOts());
            } catch (RuntimeException e) {
                log.warn("No se pudo confirmar el anclaje {}: {}",
                    anclaje.getIdAnclaje(), e.getMessage());
                continue;
            }
            if (resultado.isEmpty()) {
                continue;
            }
            ProveedorSellado.ResultadoConfirmacion confirmacion = resultado.get();
            anclaje.setEstado(EstadoAnclaje.CONFIRMADO);
            anclaje.setPruebaConfirmada(confirmacion.pruebaOts());
            anclaje.setHashBitcoin(confirmacion.hashBloqueBitcoin());
            anclaje.setBitcoinAltura(confirmacion.bitcoinAltura());
            anclaje.setFechaConfirmacion(LocalDateTime.now());
            anclaje.setFechaActualizacion(LocalDateTime.now());
            anclajeRepository.save(anclaje);
            confirmados++;
            auditoria.registrarSistema(AccionesAdmin.LEDGER_ANCLAJE_CONFIRMAR,
                "AnclajeLedger", anclaje.getIdAnclaje(),
                "Anclaje confirmado en el bloque " + confirmacion.bitcoinAltura()
                    + " de Bitcoin");
        }
        return confirmados;
    }

    @Transactional(readOnly = true)
    public Optional<AnclajeLedger> porId(Long idAnclaje) {
        return anclajeRepository.findById(idAnclaje);
    }

    @Transactional(readOnly = true)
    public List<AnclajeLedger> listar(int pagina, int tamanio) {
        return anclajeRepository.findAll(
            PageRequest.of(Math.max(0, pagina), Math.min(Math.max(1, tamanio), 100),
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC, "idAnclaje")))
            .getContent();
    }

    /** Anclaje que cubre cada evento dado (evento → anclaje). */
    @Transactional(readOnly = true)
    public Map<Long, AnclajeLedger> anclajesDeEventos(Collection<Long> idsEventos) {
        if (idsEventos == null || idsEventos.isEmpty()) {
            return Map.of();
        }
        List<AnclajeLedgerEvento> links =
            anclajeEventoRepository.findByIdEventoLedgerIn(idsEventos);
        if (links.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> eventoAAnclaje = new HashMap<>();
        for (AnclajeLedgerEvento link : links) {
            eventoAAnclaje.putIfAbsent(link.getIdEventoLedger(), link.getIdAnclaje());
        }
        Set<Long> idsAnclaje = new java.util.HashSet<>(eventoAAnclaje.values());
        Map<Long, AnclajeLedger> anclajes = new HashMap<>();
        for (AnclajeLedger anclaje : anclajeRepository.findAllById(idsAnclaje)) {
            anclajes.put(anclaje.getIdAnclaje(), anclaje);
        }
        Map<Long, AnclajeLedger> resultado = new HashMap<>();
        eventoAAnclaje.forEach((idEvento, idAnclaje) -> {
            AnclajeLedger anclaje = anclajes.get(idAnclaje);
            if (anclaje != null) {
                resultado.put(idEvento, anclaje);
            }
        });
        return resultado;
    }

    @Transactional(readOnly = true)
    public Optional<AnclajeLedger> anclajeDeEvento(Long idEventoLedger) {
        return anclajeEventoRepository
            .findFirstByIdEventoLedgerOrderByIdAnclajeDesc(idEventoLedger)
            .flatMap(link -> anclajeRepository.findById(link.getIdAnclaje()));
    }

    @Transactional(readOnly = true)
    public List<Long> eventosDeAnclaje(Long idAnclaje) {
        List<AnclajeLedgerEvento> links =
            anclajeEventoRepository.findByIdAnclajeOrderByPosicionAsc(idAnclaje);
        List<Long> ids = new ArrayList<>(links.size());
        for (AnclajeLedgerEvento link : links) {
            ids.add(link.getIdEventoLedger());
        }
        return Collections.unmodifiableList(ids);
    }

    /**
     * Prueba de inclusión de un evento dentro de la raíz de Merkle de su anclaje.
     * Permite comprobar la pertenencia sin revelar el resto del lote.
     */
    @Transactional(readOnly = true)
    public Optional<com.temipora.backend_springboot.modelo.PruebaInclusionDTO>
            pruebaInclusionDeEvento(Long idEventoLedger) {
        Optional<AnclajeLedger> anclajeOpt = anclajeDeEvento(idEventoLedger);
        if (anclajeOpt.isEmpty()) {
            return Optional.empty();
        }
        AnclajeLedger anclaje = anclajeOpt.get();
        List<Long> ids = eventosDeAnclaje(anclaje.getIdAnclaje());
        Map<Long, String> hashPorId = new HashMap<>();
        for (EventoLedger evento : eventoRepository.findAllById(ids)) {
            hashPorId.put(evento.getIdEventoLedger(), evento.getHashEvento());
        }
        List<String> hashes = new ArrayList<>(ids.size());
        int indice = -1;
        for (int i = 0; i < ids.size(); i++) {
            String hash = hashPorId.get(ids.get(i));
            if (hash == null) {
                return Optional.empty();
            }
            hashes.add(hash);
            if (ids.get(i).equals(idEventoLedger)) {
                indice = i;
            }
        }
        if (indice < 0) {
            return Optional.empty();
        }
        List<com.temipora.backend_springboot.modelo.PruebaInclusionDTO.PasoInclusionDTO>
            pasos = merkleService.prueba(hashes, indice).stream()
                .map(p -> new com.temipora.backend_springboot.modelo.PruebaInclusionDTO
                    .PasoInclusionDTO(p.hashHermano(), p.hermanoDerecha()))
                .toList();
        return Optional.of(new com.temipora.backend_springboot.modelo.PruebaInclusionDTO(
            hashPorId.get(idEventoLedger), anclaje.getHashRaiz(), pasos));
    }

    /** Mejor anclaje (confirmado por encima de pendiente) que cubre alguno de los eventos. */
    @Transactional(readOnly = true)
    public Optional<AnclajeLedger> mejorAnclajeDeEventos(Collection<Long> idsEventos) {
        AnclajeLedger mejor = null;
        for (AnclajeLedger anclaje : anclajesDeEventos(idsEventos).values()) {
            if (mejor == null || puntajeAnclaje(anclaje) > puntajeAnclaje(mejor)) {
                mejor = anclaje;
            }
        }
        return Optional.ofNullable(mejor);
    }

    private static int puntajeAnclaje(AnclajeLedger anclaje) {
        return switch (anclaje.getEstado()) {
            case CONFIRMADO -> 2;
            case PENDIENTE -> 1;
            case FALLIDO -> 0;
        };
    }

    public static AnclajeDTO aDTO(AnclajeLedger a) {
        return new AnclajeDTO(a.getIdAnclaje(), a.getHashRaiz(), a.getCantidadEventos(),
            a.getIdEventoDesde(), a.getIdEventoHasta(), a.getEstado(),
            a.getCalendarioUrl(), a.getHashBitcoin(), a.getBitcoinAltura(),
            a.getFechaSolicitud(), a.getFechaConfirmacion());
    }

    private byte[] sha256(byte[] valor) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(valor);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible.", e);
        }
    }

    private String recortar(String valor, int maximo) {
        if (valor == null || valor.length() <= maximo) {
            return valor;
        }
        return valor.substring(0, maximo);
    }
}
