package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.repositorios.EntregaArchivoRepository;
import com.temipora.backend_springboot.repositorios.GaleriaRepository;

@SpringBootTest
class GaleriaQueriesTest {

    @Autowired
    private GaleriaRepository galeriaRepository;

    @Autowired
    private EntregaArchivoRepository archivoRepository;

    @Test
    void findPaginaGaleria_sinDatos_devuelvePaginaVacia() {
        Page<Object[]> pagina = galeriaRepository.findPaginaGaleria(
            999_999L,
            EstadoPedido.FINALIZADO,
            true,
            null,
            false,
            null,
            null,
            PageRequest.of(0, 10));

        assertThat(pagina.getContent()).isEmpty();
        assertThat(pagina.getTotalElements()).isZero();
    }

    @Test
    void filtrosSinDatos_devuelvenListasVacias() {
        assertThat(galeriaRepository.serviciosDisponibles(
            999_999L, EstadoPedido.FINALIZADO, true, null, false)).isEmpty();
        assertThat(galeriaRepository.artistasDisponibles(
            999_999L, EstadoPedido.FINALIZADO, true, null, false)).isEmpty();
    }

    @Test
    void archivosDeGaleriaSinDatos_devuelvenListaVacia() {
        assertThat(archivoRepository.listarGaleriaPorItems(
            List.of(999_999L))).isEmpty();
    }

    @Test
    void archivoParaGaleriaInexistente_devuelveVacio() {
        assertThat(archivoRepository.buscarParaGaleria(999_999L)).isEmpty();
    }
}
