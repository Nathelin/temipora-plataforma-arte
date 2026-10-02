package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import com.temipora.backend_springboot.modelo.MedioPortfolio;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.ImagenRepository;

@SpringBootTest
class PortfolioQueriesTest {

	@Autowired
	private ImagenRepository imagenRepository;

	@Autowired
	private EtiquetaRepository etiquetaRepository;

	@Test
	void findParaVisor_portfolioInexistente_conFiltrosNulos_devuelveVacio() {
		var resultado = imagenRepository.findParaVisor(
				-1L, null, null, null, null, null, false, false, PageRequest.of(0, 10));

		assertThat(resultado.getContent()).isEmpty();
	}

	@Test
	void findParaVisor_portfolioInexistente_conTodosLosFiltros_devuelveVacio() {
		var resultado = imagenRepository.findParaVisor(
				-1L, 55L, "%obra%", 3L, MedioPortfolio.DIGITAL, true, true, true, PageRequest.of(0, 10));

		assertThat(resultado.getContent()).isEmpty();
	}

	@Test
	void findParaVisor_visitanteNuloConOcultamiento_activo_devuelveVacio() {
		var resultado = imagenRepository.findParaVisor(
				-1L, null, "%x%", null, null, false, true, false, PageRequest.of(0, 10));

		assertThat(resultado.getContent()).isEmpty();
	}

	@Test
	void findEtiquetasDePortfolio_portfolioInexistente_devuelveVacio() {
		assertThat(etiquetaRepository.findEtiquetasDePortfolio(-1L)).isEmpty();
	}

}
