package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.temipora.backend_springboot.modelo.ArtistaFavorito;
import com.temipora.backend_springboot.modelo.ArtistaFavoritoDTO;
import com.temipora.backend_springboot.modelo.ArtistaFavoritoId;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.PerfilArtisticoDTO;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ArtistaFavoritoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock
    private ArtistaFavoritoRepository artistaFavoritoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilArtisticoService perfilArtisticoService;

    @InjectMocks
    private FavoritoService favoritoService;

    // ---------------------------------------------------------------
    // listar
    // ---------------------------------------------------------------

    @Test
    void listar_mapeaFavoritosConPerfilArtistico() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);
        ArtistaFavorito favorito = favorito(cliente, artista, LocalDateTime.now());

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(artistaFavoritoRepository.findByUsuarioIdUsuarioOrderByFechaAgregadoDesc(1L))
            .thenReturn(List.of(favorito));
        when(perfilArtisticoService.calcularParaArtistas(List.of(2L)))
            .thenReturn(Map.of(2L, perfil()));

        List<ArtistaFavoritoDTO> resultado = favoritoService.listar("cliente");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombreUsuario()).isEqualTo("artista");
        assertThat(resultado.get(0).getTipoArtePrincipal()).isEqualTo("Acuarela");
        assertThat(resultado.get(0).getMedioPrincipal()).isEqualTo("Digital");
        assertThat(resultado.get(0).getEtiquetasDestacadas()).containsExactly("Retrato");
        assertThat(resultado.get(0).getCantidadImagenes()).isEqualTo(2);
    }

    @Test
    void listar_usuarioInexistente_lanzaNoSuchElement() {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("nadie"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoritoService.listar("nadie"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("nadie");
    }

    // ---------------------------------------------------------------
    // agregar
    // ---------------------------------------------------------------

    @Test
    void agregar_guardaFavoritoYDevuelvePerfil() {
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

        ArtistaFavoritoDTO resultado = favoritoService.agregar("cliente", "artista");

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
    void agregar_duplicado_noGuardaYDevuelveElExistente() {
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

        ArtistaFavoritoDTO resultado = favoritoService.agregar("cliente", "artista");

        verify(artistaFavoritoRepository, never()).save(any());
        assertThat(resultado.getNombreUsuario()).isEqualTo("artista");
    }

    @Test
    void agregar_perfilPropio_lanzaError() {
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));

        assertThatThrownBy(() -> favoritoService.agregar("artista", "artista"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No podés agregarte a vos mismo a tus favoritos.");

        verify(artistaFavoritoRepository, never()).save(any());
    }

    @Test
    void agregar_usuarioNoArtista_lanzaError() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario otroCliente = usuario(2L, "otro", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("otro"))
            .thenReturn(Optional.of(otroCliente));

        assertThatThrownBy(() -> favoritoService.agregar("cliente", "otro"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El usuario indicado no es un artista.");

        verify(artistaFavoritoRepository, never()).save(any());
    }

    @Test
    void agregar_artistaInactivo_lanzaError() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, false);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));

        assertThatThrownBy(() -> favoritoService.agregar("cliente", "artista"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El artista no está disponible.");

        verify(artistaFavoritoRepository, never()).save(any());
    }

    @Test
    void agregar_artistaInexistente_lanzaNoSuchElement() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("fantasma"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoritoService.agregar("cliente", "fantasma"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("fantasma");
    }

    // ---------------------------------------------------------------
    // eliminar
    // ---------------------------------------------------------------

    @Test
    void eliminar_existente_borraYDevuelveTrue() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);
        ArtistaFavorito favorito = favorito(cliente, artista, LocalDateTime.now());

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));
        when(artistaFavoritoRepository.findByUsuarioIdUsuarioAndArtistaIdUsuario(1L, 2L))
            .thenReturn(Optional.of(favorito));

        boolean resultado = favoritoService.eliminar("cliente", "artista");

        assertThat(resultado).isTrue();
        verify(artistaFavoritoRepository).delete(favorito);
    }

    @Test
    void eliminar_inexistente_devuelveFalse() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));
        when(artistaFavoritoRepository.findByUsuarioIdUsuarioAndArtistaIdUsuario(1L, 2L))
            .thenReturn(Optional.empty());

        boolean resultado = favoritoService.eliminar("cliente", "artista");

        assertThat(resultado).isFalse();
        verify(artistaFavoritoRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------
    // esFavorito
    // ---------------------------------------------------------------

    @Test
    void esFavorito_devuelveTrueCuandoExiste() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));
        when(artistaFavoritoRepository.existsByUsuarioIdUsuarioAndArtistaIdUsuario(1L, 2L))
            .thenReturn(true);

        assertThat(favoritoService.esFavorito("cliente", "artista")).isTrue();
    }

    @Test
    void esFavorito_devuelveFalseCuandoNoExiste() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE, true);
        Usuario artista = usuario(2L, "artista", ERol.ARTISTA, true);

        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente"))
            .thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("artista"))
            .thenReturn(Optional.of(artista));
        when(artistaFavoritoRepository.existsByUsuarioIdUsuarioAndArtistaIdUsuario(1L, 2L))
            .thenReturn(false);

        assertThat(favoritoService.esFavorito("cliente", "artista")).isFalse();
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
