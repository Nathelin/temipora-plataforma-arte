package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.temipora.backend_springboot.modelo.ArtistaFavorito;
import com.temipora.backend_springboot.modelo.ArtistaFavoritoDTO;
import com.temipora.backend_springboot.modelo.ArtistaFavoritoId;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.PerfilArtisticoDTO;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.ServicioFavorito;
import com.temipora.backend_springboot.modelo.ServicioFavoritoDTO;
import com.temipora.backend_springboot.modelo.ServicioFavoritoId;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ArtistaFavoritoRepository;
import com.temipora.backend_springboot.repositorios.ServicioFavoritoRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock
    private ArtistaFavoritoRepository artistaFavoritoRepository;

    @Mock
    private ServicioFavoritoRepository servicioFavoritoRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilArtisticoService perfilArtisticoService;

    @Mock
    private ServicioService servicioService;

    @InjectMocks
    private FavoritoService favoritoService;

    // ---------------------------------------------------------------
    // listarArtistas
    // ---------------------------------------------------------------

    @Test
    void listarArtistas_mapeaFavoritosConPerfilArtistico() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);
        ArtistaFavorito favorito = favorito(cliente, artista, LocalDateTime.now());

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(artistaFavoritoRepository.buscar(eq(1L), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(favorito), PageRequest.of(0, 12), 1));
        when(perfilArtisticoService.calcularParaArtistas(List.of(2L)))
            .thenReturn(Map.of(2L, perfil()));

        PaginaDTO<ArtistaFavoritoDTO> resultado = favoritoService.listarArtistas(
            "cliente", 0, 12, null, "recientes");

        assertThat(resultado.getContenido()).hasSize(1);
        assertThat(resultado.getTotalElementos()).isEqualTo(1);
        assertThat(resultado.isHayMas()).isFalse();
        assertThat(resultado.getContenido().get(0).getNombreUsuario()).isEqualTo("artista");
        assertThat(resultado.getContenido().get(0).getTipoArtePrincipal()).isEqualTo("Acuarela");
        assertThat(resultado.getContenido().get(0).getMedioPrincipal()).isEqualTo("Digital");
        assertThat(resultado.getContenido().get(0).getEtiquetasDestacadas()).containsExactly("Retrato");
        assertThat(resultado.getContenido().get(0).getCantidadImagenes()).isEqualTo(2);
    }

    @Test
    void listarArtistas_conBusqueda_normalizaElPatron() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(artistaFavoritoRepository.buscar(eq(1L), eq("%retrato%"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));
        when(perfilArtisticoService.calcularParaArtistas(List.of()))
            .thenReturn(Map.of());

        PaginaDTO<ArtistaFavoritoDTO> resultado = favoritoService.listarArtistas(
            "cliente", 0, 12, "  Retrato  ", "recientes");

        assertThat(resultado.getContenido()).isEmpty();
        assertThat(resultado.getTotalElementos()).isZero();
    }

    @Test
    void listarArtistas_ordenAlfabetico_ordenaPorNombreUsuario() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(artistaFavoritoRepository.buscar(eq(1L), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));
        when(perfilArtisticoService.calcularParaArtistas(List.of()))
            .thenReturn(Map.of());

        favoritoService.listarArtistas("cliente", 0, 12, null, "alfabetico");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(artistaFavoritoRepository).buscar(eq(1L), isNull(), captor.capture());
        Sort.Order orden = captor.getValue().getSort().getOrderFor("artista.nombreUsuario");
        assertThat(orden).isNotNull();
        assertThat(orden.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void listarArtistas_ordenInvalido_lanzaError() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> favoritoService.listarArtistas(
                "cliente", 0, 12, null, "precio"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("precio");

        verify(artistaFavoritoRepository, never()).buscar(any(), any(), any());
    }

    @Test
    void listarArtistas_usuarioInexistente_lanzaNoSuchElement() {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("nadie"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoritoService.listarArtistas(
                "nadie", 0, 12, null, "recientes"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("nadie");
    }

    // ---------------------------------------------------------------
    // agregarArtista
    // ---------------------------------------------------------------

    @Test
    void agregarArtista_guardaFavoritoYDevuelvePerfil() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));
        when(artistaFavoritoRepository.findByUsuarioIdUsuarioAndArtistaIdUsuario(1L, 2L))
            .thenReturn(Optional.empty());
        when(artistaFavoritoRepository.save(any(ArtistaFavorito.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(perfilArtisticoService.calcular(2L)).thenReturn(perfil());

        ArtistaFavoritoDTO resultado = favoritoService.agregarArtista("cliente", "artista");

        ArgumentCaptor<ArtistaFavorito> captor = ArgumentCaptor.forClass(ArtistaFavorito.class);
        verify(artistaFavoritoRepository).save(captor.capture());
        ArtistaFavorito guardado = captor.getValue();

        assertThat(guardado.getId().getIdUsuario()).isEqualTo(1L);
        assertThat(guardado.getId().getIdArtista()).isEqualTo(2L);
        assertThat(guardado.getFechaAgregado()).isNotNull();
        assertThat(resultado.getNombreUsuario()).isEqualTo("artista");
        assertThat(resultado.getTipoArtePrincipal()).isEqualTo("Acuarela");
    }

    @Test
    void agregarArtista_duplicado_noGuardaYDevuelveElExistente() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);
        ArtistaFavorito existente = favorito(cliente, artista, LocalDateTime.now());

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));
        when(artistaFavoritoRepository.findByUsuarioIdUsuarioAndArtistaIdUsuario(1L, 2L))
            .thenReturn(Optional.of(existente));
        when(perfilArtisticoService.calcular(2L)).thenReturn(perfil());

        ArtistaFavoritoDTO resultado = favoritoService.agregarArtista("cliente", "artista");

        verify(artistaFavoritoRepository, never()).save(any());
        assertThat(resultado.getNombreUsuario()).isEqualTo("artista");
    }

    @Test
    void agregarArtista_perfilPropio_lanzaError() {
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));

        assertThatThrownBy(() -> favoritoService.agregarArtista("artista", "artista"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No podés agregarte a vos mismo a tus favoritos.");

        verify(artistaFavoritoRepository, never()).save(any());
    }

    @Test
    void agregarArtista_usuarioNoArtista_lanzaError() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario otroCliente = usuario(2L, "otro", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("otro"))
            .thenReturn(Optional.of(otroCliente));

        assertThatThrownBy(() -> favoritoService.agregarArtista("cliente", "otro"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El usuario indicado no es un artista.");

        verify(artistaFavoritoRepository, never()).save(any());
    }

    @Test
    void agregarArtista_artistaInactivo_lanzaError() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, false);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));

        assertThatThrownBy(() -> favoritoService.agregarArtista("cliente", "artista"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El artista no está disponible.");

        verify(artistaFavoritoRepository, never()).save(any());
    }

    @Test
    void agregarArtista_artistaInexistente_lanzaNoSuchElement() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("fantasma"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoritoService.agregarArtista("cliente", "fantasma"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("fantasma");
    }

    // ---------------------------------------------------------------
    // eliminarArtista
    // ---------------------------------------------------------------

    @Test
    void eliminarArtista_existente_borraYDevuelveTrue() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);
        ArtistaFavorito favorito = favorito(cliente, artista, LocalDateTime.now());

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));
        when(artistaFavoritoRepository.findByUsuarioIdUsuarioAndArtistaIdUsuario(1L, 2L))
            .thenReturn(Optional.of(favorito));

        boolean resultado = favoritoService.eliminarArtista("cliente", "artista");

        assertThat(resultado).isTrue();
        verify(artistaFavoritoRepository).delete(favorito);
    }

    @Test
    void eliminarArtista_inexistente_devuelveFalse() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));
        when(artistaFavoritoRepository.findByUsuarioIdUsuarioAndArtistaIdUsuario(1L, 2L))
            .thenReturn(Optional.empty());

        boolean resultado = favoritoService.eliminarArtista("cliente", "artista");

        assertThat(resultado).isFalse();
        verify(artistaFavoritoRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------
    // esFavoritoArtista
    // ---------------------------------------------------------------

    @Test
    void esFavoritoArtista_devuelveTrueCuandoExiste() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));
        when(artistaFavoritoRepository.existsByUsuarioIdUsuarioAndArtistaIdUsuario(1L, 2L))
            .thenReturn(true);

        assertThat(favoritoService.esFavoritoArtista("cliente", "artista")).isTrue();
    }

    @Test
    void esFavoritoArtista_devuelveFalseCuandoNoExiste() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));
        when(artistaFavoritoRepository.existsByUsuarioIdUsuarioAndArtistaIdUsuario(1L, 2L))
            .thenReturn(false);

        assertThat(favoritoService.esFavoritoArtista("cliente", "artista")).isFalse();
    }

    // ---------------------------------------------------------------
    // listarServicios
    // ---------------------------------------------------------------

    @Test
    void listarServicios_mapeaFavoritosDisponibles() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);
        Servicio servicio = servicio(5L, artista, EstadoServicio.PUBLICADO);
        ServicioFavorito favorito = servicioFavorito(cliente, servicio, LocalDateTime.now());

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioFavoritoRepository.buscar(
                eq(1L), isNull(), isNull(), eq(EstadoServicio.PUBLICADO), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(favorito), PageRequest.of(0, 12), 1));
        when(servicioRepository.findByIdServicioIn(List.of(5L)))
            .thenReturn(List.of(servicio));
        when(servicioService.convertirParaVisor(List.of(servicio), 1L))
            .thenReturn(Map.of(5L, servicioDto(5L, "Retrato digital", true)));

        PaginaDTO<ServicioFavoritoDTO> resultado = favoritoService.listarServicios(
            "cliente", 0, 12, null, "recientes", "todos");

        assertThat(resultado.getContenido()).hasSize(1);
        assertThat(resultado.getTotalElementos()).isEqualTo(1);
        assertThat(resultado.getContenido().get(0).getIdServicio()).isEqualTo(5L);
        assertThat(resultado.getContenido().get(0).isDisponible()).isTrue();
        assertThat(resultado.getContenido().get(0).getServicio().getNombre())
            .isEqualTo("Retrato digital");
    }

    @Test
    void listarServicios_servicioPausado_marcaNoDisponible() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);
        Servicio servicio = servicio(5L, artista, EstadoServicio.PAUSADO);
        ServicioFavorito favorito = servicioFavorito(cliente, servicio, LocalDateTime.now());

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioFavoritoRepository.buscar(
                eq(1L), isNull(), isNull(), eq(EstadoServicio.PUBLICADO), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(favorito), PageRequest.of(0, 12), 1));
        when(servicioRepository.findByIdServicioIn(List.of(5L)))
            .thenReturn(List.of(servicio));
        when(servicioService.convertirParaVisor(List.of(servicio), 1L))
            .thenReturn(Map.of(5L, servicioDto(5L, "Retrato digital", false)));

        PaginaDTO<ServicioFavoritoDTO> resultado = favoritoService.listarServicios(
            "cliente", 0, 12, null, "recientes", "todos");

        assertThat(resultado.getContenido()).hasSize(1);
        assertThat(resultado.getContenido().get(0).isDisponible()).isFalse();
    }

    @Test
    void listarServicios_conBusquedaYEstado_pasaFiltrosNormalizados() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioFavoritoRepository.buscar(
                eq(1L), eq("%retrato%"), eq("DISPONIBLE"),
                eq(EstadoServicio.PUBLICADO), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));

        PaginaDTO<ServicioFavoritoDTO> resultado = favoritoService.listarServicios(
            "cliente", 0, 12, "  Retrato  ", "recientes", "disponibles");

        assertThat(resultado.getContenido()).isEmpty();
        verify(servicioRepository, never()).findByIdServicioIn(any());
        verify(servicioService, never()).convertirParaVisor(any(), any());
    }

    @Test
    void listarServicios_estadoNoDisponibles_pasaFiltro() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioFavoritoRepository.buscar(
                eq(1L), isNull(), eq("NO_DISPONIBLE"),
                eq(EstadoServicio.PUBLICADO), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));

        PaginaDTO<ServicioFavoritoDTO> resultado = favoritoService.listarServicios(
            "cliente", 0, 12, null, "recientes", "no-disponibles");

        assertThat(resultado.getTotalElementos()).isZero();
    }

    @Test
    void listarServicios_estadoInvalido_lanzaError() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> favoritoService.listarServicios(
                "cliente", 0, 12, null, "recientes", "archivados"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("archivados");

        verify(servicioFavoritoRepository, never()).buscar(
            any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void listarServicios_ordenAlfabetico_ordenaPorNombreDelServicio() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioFavoritoRepository.buscar(
                eq(1L), isNull(), isNull(), eq(EstadoServicio.PUBLICADO), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));

        favoritoService.listarServicios(
            "cliente", 0, 12, null, "alfabetico", "todos");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(servicioFavoritoRepository).buscar(
            eq(1L), isNull(), isNull(), eq(EstadoServicio.PUBLICADO),
            any(), any(), captor.capture());
        Sort.Order orden = captor.getValue().getSort().getOrderFor("servicio.nombre");
        assertThat(orden).isNotNull();
        assertThat(orden.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    // ---------------------------------------------------------------
    // agregarServicio
    // ---------------------------------------------------------------

    @Test
    void agregarServicio_guardaFavoritoYDevuelveDTO() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);
        Servicio servicio = servicio(5L, artista, EstadoServicio.PUBLICADO);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioRepository.findByIdServicio(5L))
            .thenReturn(Optional.of(servicio));
        when(servicioFavoritoRepository
                .findByUsuarioIdUsuarioAndServicioIdServicio(1L, 5L))
            .thenReturn(Optional.empty());
        when(servicioFavoritoRepository.save(any(ServicioFavorito.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(servicioService.convertirParaVisor(List.of(servicio), 1L))
            .thenReturn(Map.of(5L, servicioDto(5L, "Retrato digital", true)));

        ServicioFavoritoDTO resultado = favoritoService.agregarServicio("cliente", 5L);

        ArgumentCaptor<ServicioFavorito> captor = ArgumentCaptor.forClass(ServicioFavorito.class);
        verify(servicioFavoritoRepository).save(captor.capture());
        ServicioFavorito guardado = captor.getValue();

        assertThat(guardado.getId().getIdUsuario()).isEqualTo(1L);
        assertThat(guardado.getId().getIdServicio()).isEqualTo(5L);
        assertThat(guardado.getFechaAgregado()).isNotNull();
        assertThat(resultado.getIdServicio()).isEqualTo(5L);
        assertThat(resultado.getServicio().getNombre()).isEqualTo("Retrato digital");
    }

    @Test
    void agregarServicio_duplicado_noGuardaYDevuelveElExistente() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);
        Servicio servicio = servicio(5L, artista, EstadoServicio.PUBLICADO);
        ServicioFavorito existente = servicioFavorito(cliente, servicio, LocalDateTime.now());

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioRepository.findByIdServicio(5L))
            .thenReturn(Optional.of(servicio));
        when(servicioFavoritoRepository
                .findByUsuarioIdUsuarioAndServicioIdServicio(1L, 5L))
            .thenReturn(Optional.of(existente));
        when(servicioService.convertirParaVisor(List.of(servicio), 1L))
            .thenReturn(Map.of(5L, servicioDto(5L, "Retrato digital", true)));

        ServicioFavoritoDTO resultado = favoritoService.agregarServicio("cliente", 5L);

        verify(servicioFavoritoRepository, never()).save(any());
        assertThat(resultado.getIdServicio()).isEqualTo(5L);
    }

    @Test
    void agregarServicio_propio_lanzaError() {
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);
        Servicio servicio = servicio(5L, artista, EstadoServicio.PUBLICADO);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));
        when(servicioRepository.findByIdServicio(5L))
            .thenReturn(Optional.of(servicio));

        assertThatThrownBy(() -> favoritoService.agregarServicio("artista", 5L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No podés agregar tu propio servicio a tus favoritos.");

        verify(servicioFavoritoRepository, never()).save(any());
    }

    @Test
    void agregarServicio_noPublicado_lanzaError() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);
        Servicio servicio = servicio(5L, artista, EstadoServicio.PAUSADO);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioRepository.findByIdServicio(5L))
            .thenReturn(Optional.of(servicio));

        assertThatThrownBy(() -> favoritoService.agregarServicio("cliente", 5L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Solo podés agregar servicios publicados a tus favoritos.");

        verify(servicioFavoritoRepository, never()).save(any());
    }

    @Test
    void agregarServicio_inexistente_lanzaNoSuchElement() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioRepository.findByIdServicio(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoritoService.agregarServicio("cliente", 99L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("99");

        verify(servicioFavoritoRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // eliminarServicio
    // ---------------------------------------------------------------

    @Test
    void eliminarServicio_existente_borraYDevuelveTrue() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);
        Servicio servicio = servicio(5L, artista, EstadoServicio.PUBLICADO);
        ServicioFavorito favorito = servicioFavorito(cliente, servicio, LocalDateTime.now());

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioFavoritoRepository
                .findByUsuarioIdUsuarioAndServicioIdServicio(1L, 5L))
            .thenReturn(Optional.of(favorito));

        boolean resultado = favoritoService.eliminarServicio("cliente", 5L);

        assertThat(resultado).isTrue();
        verify(servicioFavoritoRepository).delete(favorito);
    }

    @Test
    void eliminarServicio_inexistente_devuelveFalse() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioFavoritoRepository
                .findByUsuarioIdUsuarioAndServicioIdServicio(1L, 5L))
            .thenReturn(Optional.empty());

        boolean resultado = favoritoService.eliminarServicio("cliente", 5L);

        assertThat(resultado).isFalse();
        verify(servicioFavoritoRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------
    // esFavoritoServicio / listarIdsServicios
    // ---------------------------------------------------------------

    @Test
    void esFavoritoServicio_devuelveTrueCuandoExiste() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioFavoritoRepository
                .existsByUsuarioIdUsuarioAndServicioIdServicio(1L, 5L))
            .thenReturn(true);

        assertThat(favoritoService.esFavoritoServicio("cliente", 5L)).isTrue();
    }

    @Test
    void listarIdsServicios_devuelveIdsDelUsuario() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(servicioFavoritoRepository.findIdsServicioByUsuario(1L))
            .thenReturn(List.of(5L, 8L));

        assertThat(favoritoService.listarIdsServicios("cliente"))
            .containsExactly(5L, 8L);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Usuario usuario(Long id, String nombre, ERol rol, boolean activo) {
        return Usuario.builder()
            .idUsuario(id)
            .nombreUsuario(nombre)
            .nombreDisplay(nombre)
            .correo(nombre + "@mail.com")
            .contrasenia("secreta")
            .estadoCuenta(activo)
            .rol(Rol.builder().idRol(1L).nombre(rol).build())
            .build();
    }

    private ArtistaFavorito favorito(Usuario usuario, Usuario artista, LocalDateTime fecha) {
        ArtistaFavorito favorito = new ArtistaFavorito();
        favorito.setId(new ArtistaFavoritoId(
            usuario.getIdUsuario(), artista.getIdUsuario()));
        favorito.setUsuario(usuario);
        favorito.setArtista(artista);
        favorito.setFechaAgregado(fecha);
        return favorito;
    }

    private Servicio servicio(Long id, Usuario artista, EstadoServicio estado) {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(id);
        servicio.setArtista(artista);
        servicio.setEstado(estado);
        servicio.setNombre("Retrato digital");
        return servicio;
    }

    private ServicioFavorito servicioFavorito(
            Usuario usuario, Servicio servicio, LocalDateTime fecha) {
        ServicioFavorito favorito = new ServicioFavorito();
        favorito.setId(new ServicioFavoritoId(
            usuario.getIdUsuario(), servicio.getIdServicio()));
        favorito.setUsuario(usuario);
        favorito.setServicio(servicio);
        favorito.setFechaAgregado(fecha);
        return favorito;
    }

    private ServicioDTO servicioDto(Long id, String nombre) {
        return servicioDto(id, nombre, false);
    }

    private ServicioDTO servicioDto(Long id, String nombre, boolean disponibleAhora) {
        return ServicioDTO.builder()
            .idServicio(id)
            .nombre(nombre)
            .nombreArtista("artista")
            .disponibleAhora(disponibleAhora)
            .build();
    }

    private PerfilArtisticoDTO perfil() {
        return PerfilArtisticoDTO.builder()
            .tipoArtePrincipal("Acuarela")
            .medioPrincipal("Digital")
            .etiquetasDestacadas(List.of("Retrato"))
            .tiposServicioDestacados(List.of("Comisión"))
            .cantidadImagenes(2)
            .cantidadServicios(1)
            .build();
    }
}
