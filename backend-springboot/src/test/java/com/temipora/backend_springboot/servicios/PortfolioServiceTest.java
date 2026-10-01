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
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.temipora.backend_springboot.modelo.ContenidoSensible;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Imagen;
import com.temipora.backend_springboot.modelo.ImagenPortfolioDTO;
import com.temipora.backend_springboot.modelo.ImagenPortfolioRequest;
import com.temipora.backend_springboot.modelo.MedioPortfolio;
import com.temipora.backend_springboot.modelo.Portfolio;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ImagenRepository;
import com.temipora.backend_springboot.repositorios.PortfolioRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    // ---------------------------------------------------------------
    // listar
    // ---------------------------------------------------------------

    @Test
    void listar_artistaConPortfolioYImagenes_devuelveDTOS() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        Imagen imagen = imagenDe(portfolio, artista, "Obra 1", null);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Artista777"))
                .thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(portfolio));
        when(imagenRepository.findByPortfolioAndEstadoTrueOrderByFechaSubidaDesc(portfolio))
                .thenReturn(List.of(imagen));

        List<ImagenPortfolioDTO> resultado = portfolioService.listar("Artista777");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTitulo()).isEqualTo("Obra 1");
        assertThat(resultado.get(0).isRequiereAdvertencia()).isFalse();
        verify(imagenRepository)
                .findByPortfolioAndEstadoTrueOrderByFechaSubidaDesc(portfolio);
    }

    @Test
    void listar_artistaSinPortfolio_devuelveListaVacia() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Artista777"))
                .thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.empty());

        List<ImagenPortfolioDTO> resultado = portfolioService.listar("Artista777");

        assertThat(resultado).isEmpty();
        verify(imagenRepository, never())
                .findByPortfolioAndEstadoTrueOrderByFechaSubidaDesc(any());
    }

    @Test
    void listar_usuarioInexistente_lanzaNoSuchElement() {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("fantasma"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.listar("fantasma"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Usuario no encontrado: fantasma");
    }

    @Test
    void listar_usuarioNoArtista_lanzaIllegalArgument() {
        Usuario cliente = usuarioConRol(7L, "cliente42", ERol.CLIENTE);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente42"))
                .thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> portfolioService.listar("cliente42"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El usuario no es un artista.");
    }

    // ---------------------------------------------------------------
    // listarParaVisor
    // ---------------------------------------------------------------

    @Test
    void listarParaVisor_sinVisitante_prefPorDefectoYMarcaAdvertencia() {
        Imagen sensible = imagenDe(null, null, "Sensible", ContenidoSensible.DESNUDOS);
        when(imagenRepository.findVisibleForViewer(10L, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(sensible), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, null, PAGEABLE);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isRequiereAdvertencia()).isTrue();
        verify(usuarioRepository, never()).findByIdUsuario(any());
        verify(imagenRepository, never())
                .findVisibleForViewerOcultandoSensibles(any(), any(), any());
    }

    @Test
    void listarParaVisor_visitanteConPrefOcultar_usaConsultaDeOcultamiento() {
        Usuario visitante = usuarioConPref(55L, EPreferenciaContenidoSensible.OCULTAR);
        Imagen sensible = imagenDe(null, null, "Sensible", ContenidoSensible.VIOLENCIA);
        when(usuarioRepository.findByIdUsuario(55L)).thenReturn(Optional.of(visitante));
        when(imagenRepository.findVisibleForViewerOcultandoSensibles(10L, 55L, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(sensible), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, 55L, PAGEABLE);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isRequiereAdvertencia()).isFalse();
        verify(imagenRepository, never()).findVisibleForViewer(any(), any());
    }

    @Test
    void listarParaVisor_visitanteNoRegistrado_prefPorDefectoADVERTIR() {
        Imagen sinSensibilidad = imagenDe(null, null, "Normal", null);
        when(usuarioRepository.findByIdUsuario(55L)).thenReturn(Optional.empty());
        when(imagenRepository.findVisibleForViewer(10L, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(sinSensibilidad), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, 55L, PAGEABLE);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isRequiereAdvertencia()).isFalse();
        verify(imagenRepository, never())
                .findVisibleForViewerOcultandoSensibles(any(), any(), any());
    }

    @Test
    void listarParaVisor_prefMostrarSinAdvertencia_noMarcaAdvertencia() {
        Usuario visitante = usuarioConPref(55L, EPreferenciaContenidoSensible.MOSTRAR_SIN_ADVERTENCIA);
        Imagen sensible = imagenDe(null, null, "Sensible", ContenidoSensible.DESNUDOS);
        when(usuarioRepository.findByIdUsuario(55L)).thenReturn(Optional.of(visitante));
        when(imagenRepository.findVisibleForViewer(10L, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(sensible), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, 55L, PAGEABLE);

        assertThat(resultado.getContent().get(0).isRequiereAdvertencia()).isFalse();
        verify(imagenRepository, never())
                .findVisibleForViewerOcultandoSensibles(any(), any(), any());
    }

    @Test
    void listarParaVisor_visitanteEsDuenoDeLaImagen_noMarcaAdvertencia() {
        Usuario duenio = usuarioConPref(55L, EPreferenciaContenidoSensible.ADVERTIR);
        Imagen sensible = imagenDe(null, duenio, "Sensible", ContenidoSensible.GENERAL);
        when(usuarioRepository.findByIdUsuario(55L)).thenReturn(Optional.of(duenio));
        when(imagenRepository.findVisibleForViewer(10L, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(sensible), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, 55L, PAGEABLE);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isRequiereAdvertencia()).isFalse();
    }

    // ---------------------------------------------------------------
    // listarParaVisorPorNombreUsuario
    // ---------------------------------------------------------------

    @Test
    void listarParaVisorPorNombreUsuario_sinPortfolio_devuelvePaginaVacia() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Artista777"))
                .thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.empty());

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisorPorNombreUsuario("Artista777", null, PAGEABLE);

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
        verify(imagenRepository, never()).findVisibleForViewer(any(), any());
    }

    @Test
    void listarParaVisorPorNombreUsuario_conPortfolio_delegaEnListarParaVisor() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        Imagen imagen = imagenDe(portfolio, artista, "Obra 1", null);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Artista777"))
                .thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(portfolio));
        when(imagenRepository.findVisibleForViewer(10L, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(imagen), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisorPorNombreUsuario("Artista777", null, PAGEABLE);

        assertThat(resultado.getContent()).hasSize(1);
        verify(imagenRepository).findVisibleForViewer(10L, PAGEABLE);
    }

    // ---------------------------------------------------------------
    // crear
    // ---------------------------------------------------------------

    @Test
    void crear_artistaInexistente_lanzaNoSuchElement() {
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.crear(99L, requestValido()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Artista no encontrado: 99");
    }

    @Test
    void crear_usuarioNoArtista_lanzaIllegalArgument() {
        Usuario cliente = usuarioConRol(7L, "cliente42", ERol.CLIENTE);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> portfolioService.crear(7L, requestValido()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El usuario no es un artista.");
        verify(portfolioRepository, never()).findByArtista(any());
    }

    @Test
    void crear_sinPortfolio_loCreaYGuardaImagenConTitulosRecortados() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio creado = portfolioDe(artista);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.empty());
        when(portfolioRepository.save(any(Portfolio.class))).thenReturn(creado);
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndMedioAndEstadoTrue(
                eq(creado), eq("Mi obra"), eq(MedioPortfolio.DIGITAL))).thenReturn(false);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(99L);
            return imagen;
        });

        ImagenPortfolioRequest request = requestValido();
        request.setTitulo("  Mi obra  ");
        request.setUrl("  https://x/img.png  ");
        request.setDescripcion("  Una obra  ");
        request.setMateriales(null);
        request.setContenidoSensible(ContenidoSensible.DESNUDOS);

        ImagenPortfolioDTO dto = portfolioService.crear(7L, request);

        ArgumentCaptor<Portfolio> portfolioCaptor = ArgumentCaptor.forClass(Portfolio.class);
        verify(portfolioRepository).save(portfolioCaptor.capture());
        assertThat(portfolioCaptor.getValue().getArtista()).isSameAs(artista);

        ArgumentCaptor<Imagen> imagenCaptor = ArgumentCaptor.forClass(Imagen.class);
        verify(imagenRepository).save(imagenCaptor.capture());
        Imagen guardada = imagenCaptor.getValue();
        assertThat(guardada.getTitulo()).isEqualTo("Mi obra");
        assertThat(guardada.getUrl()).isEqualTo("https://x/img.png");
        assertThat(guardada.getDescripcion()).isEqualTo("Una obra");
        assertThat(guardada.getMateriales()).isNull();
        assertThat(guardada.getContenidoSensible()).isEqualTo(ContenidoSensible.DESNUDOS);
        assertThat(guardada.isEstado()).isTrue();
        assertThat(guardada.getFechaSubida()).isNotNull();
        assertThat(guardada.getPortfolio()).isSameAs(creado);
        assertThat(guardada.getUsuario()).isSameAs(artista);

        assertThat(dto.getIdImagen()).isEqualTo(99L);
        assertThat(dto.getTitulo()).isEqualTo("Mi obra");
    }

    @Test
    void crear_conPortfolioExistente_loReutilizaYRecortaMateriales() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio existente = portfolioDe(artista);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(existente));
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndMedioAndEstadoTrue(
                eq(existente), eq("Mi obra"), eq(MedioPortfolio.DIGITAL))).thenReturn(false);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(99L);
            return imagen;
        });

        ImagenPortfolioRequest request = requestValido();
        request.setMateriales("  lapiz y acuarela  ");

        ImagenPortfolioDTO dto = portfolioService.crear(7L, request);

        verify(portfolioRepository, never()).save(any(Portfolio.class));
        ArgumentCaptor<Imagen> imagenCaptor = ArgumentCaptor.forClass(Imagen.class);
        verify(imagenRepository).save(imagenCaptor.capture());
        assertThat(imagenCaptor.getValue().getMateriales()).isEqualTo("lapiz y acuarela");
        assertThat(imagenCaptor.getValue().getPortfolio()).isSameAs(existente);
        assertThat(dto.getIdImagen()).isEqualTo(99L);
    }

    @Test
    void crear_tituloDuplicado_lanzaIllegalArgumentYNoGuarda() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio existente = portfolioDe(artista);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(existente));
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndMedioAndEstadoTrue(
                eq(existente), eq("Mi obra"), eq(MedioPortfolio.DIGITAL))).thenReturn(true);

        assertThatThrownBy(() -> portfolioService.crear(7L, requestValido()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una imagen con ese título y medio en el portfolio.");

        verify(imagenRepository, never()).save(any(Imagen.class));
    }

    // ---------------------------------------------------------------
    // nombreUsuario
    // ---------------------------------------------------------------

    @Test
    void nombreUsuario_existente_devuelveNombre() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));

        assertThat(portfolioService.nombreUsuario(7L)).isEqualTo("Artista777");
    }

    @Test
    void nombreUsuario_inexistente_lanzaNoSuchElement() {
        when(usuarioRepository.findByIdUsuario(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.nombreUsuario(42L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Usuario no encontrado: 42");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Usuario usuarioArtista(Long id, String nombreUsuario) {
        return usuarioConRol(id, nombreUsuario, ERol.ARTISTA);
    }

    private Usuario usuarioConPref(Long id, EPreferenciaContenidoSensible pref) {
        return Usuario.builder()
                .idUsuario(id)
                .nombreUsuario("visitante" + id)
                .correo("visitante" + id + "@mail.com")
                .rol(Rol.builder().idRol(3L).nombre(ERol.CLIENTE).build())
                .preferenciaSensible(pref)
                .build();
    }

    private Usuario usuarioConRol(Long id, String nombreUsuario, ERol rol) {
        return Usuario.builder()
                .idUsuario(id)
                .nombreUsuario(nombreUsuario)
                .correo(nombreUsuario.toLowerCase() + "@mail.com")
                .rol(Rol.builder().idRol(2L).nombre(rol).build())
                .build();
    }

    private Portfolio portfolioDe(Usuario artista) {
        Portfolio portfolio = new Portfolio();
        portfolio.setIdPortfolio(10L);
        portfolio.setArtista(artista);
        return portfolio;
    }

    private Imagen imagenDe(Portfolio portfolio, Usuario autor, String titulo,
            ContenidoSensible contenidoSensible) {
        Imagen imagen = new Imagen();
        imagen.setIdImagen(5L);
        imagen.setUrl("https://x/img.png");
        imagen.setTitulo(titulo);
        imagen.setDescripcion("descripcion");
        imagen.setFechaSubida(LocalDateTime.of(2026, 1, 1, 10, 0));
        imagen.setMedio(MedioPortfolio.DIGITAL);
        imagen.setEstado(true);
        imagen.setContenidoSensible(contenidoSensible);
        imagen.setPortfolio(portfolio);
        imagen.setUsuario(autor);
        return imagen;
    }

    private ImagenPortfolioRequest requestValido() {
        ImagenPortfolioRequest request = new ImagenPortfolioRequest();
        request.setTitulo("Mi obra");
        request.setDescripcion("Una obra");
        request.setUrl("https://x/img.png");
        request.setMedio(MedioPortfolio.DIGITAL);
        return request;
    }
}
