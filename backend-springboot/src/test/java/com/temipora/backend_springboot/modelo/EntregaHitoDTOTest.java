package com.temipora.backend_springboot.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class EntregaHitoDTOTest {

    private final LocalDateTime fecha = LocalDateTime.of(2026, 4, 1, 10, 0);

    @Test
    void desdeEntrega_mapeaAutorArchivosYRevision() {
        Usuario autor = Usuario.builder().idUsuario(2L).nombreUsuario("artista").build();
        Usuario revisor = Usuario.builder().idUsuario(3L).nombreUsuario("cliente").build();

        EntregaHito entrega = new EntregaHito();
        entrega.setIdEntrega(5L);
        entrega.setNumero(2);
        entrega.setFechaEntrega(fecha);
        entrega.setAutor(autor);
        entrega.setDescripcion("Entrega");
        entrega.setVigente(true);
        entrega.setEsFinal(true);
        entrega.setRevisionMotivo("Cambiar fondo");
        entrega.setRevisionFecha(fecha.plusDays(1));
        entrega.setRevisionPor(revisor);

        EntregaHitoDTO dto = EntregaHitoDTO.desdeEntrega(
            entrega, List.of(archivo(1L, false), archivo(2L, true)), 10L, true, false);

        assertThat(dto.getIdEntrega()).isEqualTo(5L);
        assertThat(dto.getNumero()).isEqualTo(2);
        assertThat(dto.getAutor().getNombreUsuario()).isEqualTo("artista");
        assertThat(dto.getDescripcion()).isEqualTo("Entrega");
        assertThat(dto.isVigente()).isTrue();
        assertThat(dto.isEsFinal()).isTrue();
        assertThat(dto.isAprobada()).isFalse();
        assertThat(dto.getRevisionMotivo()).isEqualTo("Cambiar fondo");
        assertThat(dto.getRevisionPor().getNombreUsuario()).isEqualTo("cliente");
        assertThat(dto.getArchivos()).hasSize(1);
    }

    @Test
    void desdeEntrega_filtraArchivosRetirados() {
        EntregaHito entrega = entregaBasica();

        EntregaHitoDTO dto = EntregaHitoDTO.desdeEntrega(
            entrega, List.of(archivo(1L, true)), 10L, false, true);

        assertThat(dto.getArchivos()).isEmpty();
        assertThat(dto.isAprobada()).isTrue();
    }

    @Test
    void desdeEntrega_sinAutorNiRevisionPor_dejaResumenesNulos() {
        EntregaHito entrega = entregaBasica();
        entrega.setAutor(null);
        entrega.setRevisionPor(null);

        EntregaHitoDTO dto = EntregaHitoDTO.desdeEntrega(
            entrega, List.of(), 10L, false, false);

        assertThat(dto.getAutor()).isNull();
        assertThat(dto.getRevisionPor()).isNull();
    }

    @Test
    void entregaArchivoDTO_incluyeUrlsSegunConfiguracion() {
        EntregaArchivo archivo = archivo(9L, false);
        archivo.setClavePreview("preview.webp");

        EntregaArchivoDTO conOriginal = EntregaArchivoDTO.desdeArchivo(
            archivo, 10L, 5L, true);
        EntregaArchivoDTO sinOriginal = EntregaArchivoDTO.desdeArchivo(
            archivo, 10L, 5L, false);

        assertThat(conOriginal.getUrlOriginal()).contains("/original");
        assertThat(conOriginal.getUrlPreview()).contains("/preview");
        assertThat(sinOriginal.getUrlOriginal()).isNull();
        assertThat(conOriginal.getNombre()).isEqualTo("archivo.png");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private EntregaHito entregaBasica() {
        EntregaHito entrega = new EntregaHito();
        entrega.setIdEntrega(5L);
        entrega.setNumero(1);
        entrega.setFechaEntrega(fecha);
        entrega.setAutor(Usuario.builder().idUsuario(2L).nombreUsuario("artista").build());
        return entrega;
    }

    private EntregaArchivo archivo(Long id, boolean retirado) {
        EntregaArchivo archivo = new EntregaArchivo();
        archivo.setIdArchivo(id);
        archivo.setNombreOriginal("archivo.png");
        archivo.setMime("image/png");
        archivo.setAncho(100);
        archivo.setAlto(100);
        archivo.setSha256("abc");
        archivo.setOrden(0);
        archivo.setRetirado(retirado);
        return archivo;
    }
}
