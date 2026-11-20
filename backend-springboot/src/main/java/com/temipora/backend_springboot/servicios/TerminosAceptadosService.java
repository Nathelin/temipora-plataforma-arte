package com.temipora.backend_springboot.servicios;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.temipora.backend_springboot.modelo.AceptacionTerminosPlataforma;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.Terminos;
import com.temipora.backend_springboot.modelo.TerminosAceptadosDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;

/**
 * Acceso público a la aceptación de términos de un pedido. El código incluye la
 * huella del texto aceptado, así que sólo quien lo conoce puede consultarla.
 */
@Service
public class TerminosAceptadosService {

    private static final String NO_ENCONTRADO =
        "No encontramos una aceptación de términos con ese código.";

    private final SolicitudRepository solicitudRepository;
    private final PedidoRepository pedidoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String urlFrontend;

    public TerminosAceptadosService(
            SolicitudRepository solicitudRepository,
            PedidoRepository pedidoRepository,
            @Value("${app.frontend.url:http://localhost:5173}")
            String urlFrontend) {
        this.solicitudRepository = solicitudRepository;
        this.pedidoRepository = pedidoRepository;
        this.urlFrontend = (urlFrontend != null ? urlFrontend : "")
            .replaceAll("/+$", "");
    }

    @Transactional(readOnly = true)
    public TerminosAceptadosDTO obtener(String codigo) {
        CodigoTerminos.Datos datos = CodigoTerminos.decodificar(codigo)
            .orElseThrow(() -> new NoSuchElementException(NO_ENCONTRADO));
        Solicitud solicitud = solicitudRepository
            .findByIdSolicitud(datos.idSolicitud())
            .orElseThrow(() -> new NoSuchElementException(NO_ENCONTRADO));

        String huella = HuellaTexto.sha256(solicitud.getTerminosSnapshot());
        if (huella == null || !huella.startsWith(datos.huella())) {
            throw new NoSuchElementException(NO_ENCONTRADO);
        }

        Optional<Pedido> pedido = pedidoRepository
            .findBySolicitudIdSolicitud(solicitud.getIdSolicitud());
        AceptacionTerminosPlataforma plataforma = pedido
            .map(Pedido::getAceptacionTerminosPlataforma)
            .orElse(null);
        Terminos terminos = solicitud.getTerminos();
        String codigoPublico = CodigoTerminos.codificar(
            solicitud.getIdSolicitud(), huella);

        return TerminosAceptadosDTO.builder()
            .codigo(codigoPublico)
            .urlVerificacion(urlFrontend + "/terminos-aceptados/"
                + codigoPublico)
            .idPedido(pedido.map(Pedido::getIdPedido).orElse(null))
            .cliente(usuario(solicitud.getCliente()))
            .artista(usuario(solicitud.getArtista()))
            .servicio(solicitud.getServicio() != null
                ? solicitud.getServicio().getNombre()
                : null)
            .version(solicitud.getTerminosVersion())
            .delServicio(terminos != null && terminos.getServicio() != null)
            .aceptadosEn(solicitud.getTerminosAceptadosEn())
            .huella(huella)
            .plataformaVersion(plataforma != null
                ? plataforma.getVersion()
                : null)
            .plataformaTitulo(plataforma != null
                    && plataforma.getTerminosPlataforma() != null
                ? plataforma.getTerminosPlataforma().getTitulo()
                : null)
            .plataformaAceptadosEn(plataforma != null
                ? plataforma.getFechaAceptacion()
                : null)
            .plataformaHuella(plataforma != null
                ? HuellaTexto.sha256(plataforma.getSnapshot())
                : null)
            .contenido(contenido(solicitud.getTerminosSnapshot()))
            .build();
    }

    private TerminosAceptadosDTO.Contenido contenido(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(
                snapshot, TerminosAceptadosDTO.Contenido.class);
        } catch (JacksonException e) {
            return null;
        }
    }

    private static String usuario(Usuario usuario) {
        return usuario != null ? usuario.getNombreUsuario() : null;
    }
}
