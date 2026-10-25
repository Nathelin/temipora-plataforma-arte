package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.temipora.backend_springboot.modelo.ArtistaFavorito;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.ServicioFavorito;
import com.temipora.backend_springboot.repositorios.ArtistaFavoritoRepository;
import com.temipora.backend_springboot.repositorios.ServicioFavoritoRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;

@SpringBootTest
class FavoritoQueriesTest {

	@Autowired
	private ArtistaFavoritoRepository artistaFavoritoRepository;

	@Autowired
	private ServicioFavoritoRepository servicioFavoritoRepository;

	@Autowired
	private ServicioRepository servicioRepository;

	@Test
	void buscarArtistas_usuarioInexistente_devuelvePaginaVacia() {
		Page<ArtistaFavorito> pagina = artistaFavoritoRepository.buscar(
			-1L,
			null,
			PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fechaAgregado")));

		assertThat(pagina.getContent()).isEmpty();
		assertThat(pagina.getTotalElements()).isZero();
	}

	@Test
	void buscarArtistas_conBusquedaYOrdenAlfabetico_esValido() {
		Page<ArtistaFavorito> pagina = artistaFavoritoRepository.buscar(
			-1L,
			"%retrato%",
			PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "artista.nombreUsuario")));

		assertThat(pagina.getContent()).isEmpty();
		assertThat(pagina.getTotalElements()).isZero();
	}

	@Test
	void buscarServicios_usuarioInexistente_devuelvePaginaVacia() {
		Page<ServicioFavorito> pagina = servicioFavoritoRepository.buscar(
			-1L,
			null,
			null,
			EstadoServicio.PUBLICADO,
			PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fechaAgregado")));

		assertThat(pagina.getContent()).isEmpty();
		assertThat(pagina.getTotalElements()).isZero();
	}

	@Test
	void buscarServicios_conBusquedaOrdenYFiltroDisponibles_esValido() {
		Page<ServicioFavorito> pagina = servicioFavoritoRepository.buscar(
			-1L,
			"%retrato%",
			"DISPONIBLE",
			EstadoServicio.PUBLICADO,
			PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "servicio.nombre")));

		assertThat(pagina.getContent()).isEmpty();
		assertThat(pagina.getTotalElements()).isZero();
	}

	@Test
	void buscarServicios_noDisponibles_esValido() {
		Page<ServicioFavorito> pagina = servicioFavoritoRepository.buscar(
			-1L,
			null,
			"NO_DISPONIBLE",
			EstadoServicio.PUBLICADO,
			PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fechaAgregado")));

		assertThat(pagina.getContent()).isEmpty();
		assertThat(pagina.getTotalElements()).isZero();
	}

	@Test
	void findByIdServicioIn_inexistente_devuelveVacio() {
		assertThat(servicioRepository.findByIdServicioIn(List.of(-1L))).isEmpty();
	}

	@Test
	void findIdsServicioByUsuario_usuarioInexistente_devuelveVacio() {
		assertThat(servicioFavoritoRepository.findIdsServicioByUsuario(-1L))
			.isEmpty();
	}
}
