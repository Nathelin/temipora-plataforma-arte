package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.TerminosPlataforma;
import com.temipora.backend_springboot.modelo.TerminosPlataformaDTO;
import com.temipora.backend_springboot.modelo.TerminosPlataformaRequest;
import com.temipora.backend_springboot.repositorios.TerminosPlataformaRepository;
import com.temipora.backend_springboot.servicios.TerminosPlataformaService;

/**
 * Publica dos versiones seguidas contra la base real. Sin el flush previo al
 * INSERT, la versión nueva choca con uk_terminos_plataforma_vigente porque
 * Hibernate ejecuta el INSERT antes que el UPDATE de la anterior.
 */
@SpringBootTest
@Transactional
class TerminosPlataformaIntegracionTest {

    @Autowired
    private TerminosPlataformaService service;

    @Autowired
    private TerminosPlataformaRepository repository;

    @Test
    void publicarDosVecesDejaUnaSolaVersionVigente() {
        TerminosPlataformaDTO primera = service.publicar(
            request("Términos de prueba A", "Contenido A"));
        TerminosPlataformaDTO segunda = service.publicar(
            request("Términos de prueba B", "Contenido B"));

        assertThat(segunda.getVersion()).isEqualTo(primera.getVersion() + 1);

        var vigentes = repository.findByVigenteTrue();
        assertThat(vigentes).hasSize(1);
        assertThat(vigentes.get(0).getVersion()).isEqualTo(segunda.getVersion());

        TerminosPlataforma anterior = repository.findByVersion(primera.getVersion())
            .orElseThrow();
        assertThat(anterior.isVigente()).isFalse();
    }

    private TerminosPlataformaRequest request(String titulo, String contenido) {
        TerminosPlataformaRequest request = new TerminosPlataformaRequest();
        request.setTitulo(titulo);
        request.setContenido(contenido);
        return request;
    }
}
