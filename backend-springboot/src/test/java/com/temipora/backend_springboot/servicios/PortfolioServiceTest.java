package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

import com.temipora.backend_springboot.modelo.CategoriaEtiqueta;
import com.temipora.backend_springboot.modelo.ContenidoSensible;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.EntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.FiltroPortfolioRequest;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Imagen;
import com.temipora.backend_springboot.modelo.ImagenEtiqueta;
import com.temipora.backend_springboot.modelo.ImagenMaterial;
import com.temipora.backend_springboot.modelo.ImagenPortfolioDTO;
import com.temipora.backend_springboot.modelo.ImagenPortfolioRequest;
import com.temipora.backend_springboot.modelo.ImagenPortfolioVerificadaRequest;
import com.temipora.backend_springboot.modelo.Material;
import com.temipora.backend_springboot.modelo.MaterialDTO;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Portfolio;
import com.temipora.backend_springboot.modelo.PortfolioEntregaVerificada;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.TipoArte;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.EntregaArchivoRepository;
import com.temipora.backend_springboot.repositorios.ImagenEtiquetaRepository;
import com.temipora.backend_springboot.repositorios.ImagenMaterialRepository;
import com.temipora.backend_springboot.repositorios.ImagenRepository;
import com.temipora.backend_springboot.repositorios.MaterialRepository;
import com.temipora.backend_springboot.repositorios.PortfolioEntregaVerificadaRepository;
import com.temipora.backend_springboot.repositorios.PortfolioRepository;
import com.temipora.backend_springboot.repositorios.TipoArteRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private EtiquetaRepository etiquetaRepository;

    @Mock
    private ImagenEtiquetaRepository imagenEtiquetaRepository;

    @Mock
    private TipoArteRepository tipoArteRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private ImagenMaterialRepository imagenMaterialRepository;

    @Mock
    private PortfolioEntregaVerificadaRepository verificacionRepository;

    @Mock
    private EntregaArchivoRepository archivoRepository;

    @Mock
    private ImagenGaleriaService imagenGaleriaService;

    @InjectMocks
    private PortfolioService portfolioService;

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    private final TipoArte tipoArte = TipoArte.builder()
            .idTipoArte(1L)
            .nombre("Digital")
            .medio(medio(1L, "Digital"))
            .activo(true)
            .build();

    private void stubTipoArte() {
        when(tipoArteRepository.findByIdTipoArte(1L)).thenReturn(Optional.of(tipoArte));
    }

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
    void listar_poblaLasEtiquetasDeCadaImagen() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        Imagen imagen = imagenDe(portfolio, artista, "Obra 1", null);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Artista777"))
                .thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(portfolio));
        when(imagenRepository.findByPortfolioAndEstadoTrueOrderByFechaSubidaDesc(portfolio))
                .thenReturn(List.of(imagen));
        when(imagenEtiquetaRepository.findByImagenIdImagenIn(List.of(5L)))
                .thenReturn(List.of(fila(imagen, etiquetaDe(1L, "Acuarela", EtiquetaEstado.VALIDA, null))));

        List<ImagenPortfolioDTO> resultado = portfolioService.listar("Artista777");

        assertThat(resultado.get(0).getEtiquetas())
                .extracting(EtiquetaDTO::getNombre)
                .containsExactly("Acuarela");
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
    void listar_portfolioSinImagenes_noConsultaEtiquetas() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Artista777"))
                .thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(portfolio));
        when(imagenRepository.findByPortfolioAndEstadoTrueOrderByFechaSubidaDesc(portfolio))
                .thenReturn(List.of());

        List<ImagenPortfolioDTO> resultado = portfolioService.listar("Artista777");

        assertThat(resultado).isEmpty();
        verify(imagenEtiquetaRepository, never()).findByImagenIdImagenIn(any());
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
        when(imagenRepository.findParaVisor(eq(10L), isNull(), isNull(), isNull(), isNull(), isNull(), eq(false), eq(false), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(List.of(sensible), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, null, filtroVacio(), PAGEABLE);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isRequiereAdvertencia()).isTrue();
        verify(usuarioRepository, never()).findByIdUsuario(any());
        verify(imagenRepository, never())
                .findParaVisor(any(), any(), any(), any(), any(), any(), eq(true), anyBoolean(), any());
    }

    @Test
    void listarParaVisor_visitanteConPrefOcultar_usaConsultaDeOcultamiento() {
        Usuario visitante = usuarioConPref(55L, EPreferenciaContenidoSensible.OCULTAR);
        Imagen sensible = imagenDe(null, null, "Sensible", ContenidoSensible.VIOLENCIA);
        when(usuarioRepository.findByIdUsuario(55L)).thenReturn(Optional.of(visitante));
        when(imagenRepository.findParaVisor(eq(10L), eq(55L), isNull(), isNull(), isNull(), isNull(), eq(true), eq(false), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(List.of(sensible), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, 55L, filtroVacio(), PAGEABLE);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isRequiereAdvertencia()).isFalse();
        verify(imagenRepository, never()).findParaVisor(any(), any(), any(), any(), any(), any(), eq(false), anyBoolean(), any());
    }

    @Test
    void listarParaVisor_visitanteNoRegistrado_prefPorDefectoADVERTIR() {
        Imagen sinSensibilidad = imagenDe(null, null, "Normal", null);
        when(usuarioRepository.findByIdUsuario(55L)).thenReturn(Optional.empty());
        when(imagenRepository.findParaVisor(eq(10L), eq(55L), isNull(), isNull(), isNull(), isNull(), eq(false), eq(false), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(List.of(sinSensibilidad), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, 55L, filtroVacio(), PAGEABLE);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isRequiereAdvertencia()).isFalse();
        verify(imagenRepository, never())
                .findParaVisor(any(), any(), any(), any(), any(), any(), eq(true), anyBoolean(), any());
    }

    @Test
    void listarParaVisor_prefMostrarSinAdvertencia_noMarcaAdvertencia() {
        Usuario visitante = usuarioConPref(55L, EPreferenciaContenidoSensible.MOSTRAR_SIN_ADVERTENCIA);
        Imagen sensible = imagenDe(null, null, "Sensible", ContenidoSensible.DESNUDOS);
        when(usuarioRepository.findByIdUsuario(55L)).thenReturn(Optional.of(visitante));
        when(imagenRepository.findParaVisor(eq(10L), eq(55L), isNull(), isNull(), isNull(), isNull(), eq(false), eq(false), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(List.of(sensible), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, 55L, filtroVacio(), PAGEABLE);

        assertThat(resultado.getContent().get(0).isRequiereAdvertencia()).isFalse();
        verify(imagenRepository, never())
                .findParaVisor(any(), any(), any(), any(), any(), any(), eq(true), anyBoolean(), any());
    }

    @Test
    void listarParaVisor_visitanteEsDuenoDeLaImagen_noMarcaAdvertencia() {
        Usuario duenio = usuarioConPref(55L, EPreferenciaContenidoSensible.ADVERTIR);
        Imagen sensible = imagenDe(null, duenio, "Sensible", ContenidoSensible.GENERAL);
        when(usuarioRepository.findByIdUsuario(55L)).thenReturn(Optional.of(duenio));
        when(imagenRepository.findParaVisor(eq(10L), eq(55L), isNull(), isNull(), isNull(), isNull(), eq(false), eq(false), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(List.of(sensible), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, 55L, filtroVacio(), PAGEABLE);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isRequiereAdvertencia()).isFalse();
    }

    @Test
    void listarParaVisor_poblaLasEtiquetasDeCadaImagen() {
        Imagen imagen = imagenDe(null, null, "Obra 1", null);
        when(imagenRepository.findParaVisor(eq(10L), isNull(), isNull(), isNull(), isNull(), isNull(), eq(false), eq(false), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(List.of(imagen), PAGEABLE, 1));
        when(imagenEtiquetaRepository.findByImagenIdImagenIn(List.of(5L)))
                .thenReturn(List.of(fila(imagen, etiquetaDe(1L, "Pixel", EtiquetaEstado.VALIDA, null))));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, null, filtroVacio(), PAGEABLE);

        assertThat(resultado.getContent().get(0).getEtiquetas())
                .extracting(EtiquetaDTO::getNombre)
                .containsExactly("Pixel");
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
                .listarParaVisorPorNombreUsuario("Artista777", null, filtroVacio(), PAGEABLE);

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
        verify(imagenRepository, never()).findParaVisor(any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void listarParaVisorPorNombreUsuario_conPortfolio_delegaEnListarParaVisor() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        Imagen imagen = imagenDe(portfolio, artista, "Obra 1", null);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Artista777"))
                .thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(portfolio));
        when(imagenRepository.findParaVisor(eq(10L), isNull(), isNull(), isNull(), isNull(), isNull(), eq(false), eq(false), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(List.of(imagen), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisorPorNombreUsuario("Artista777", null, filtroVacio(), PAGEABLE);

        assertThat(resultado.getContent()).hasSize(1);
        verify(imagenRepository).findParaVisor(eq(10L), isNull(), isNull(), isNull(), isNull(), isNull(), eq(false), eq(false), eq(PAGEABLE));
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
        stubTipoArte();
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                eq(creado), eq("Mi obra"), eq(tipoArte))).thenReturn(false);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(99L);
            return imagen;
        });

        ImagenPortfolioRequest request = requestValido();
        request.setTitulo("  Mi obra  ");
        request.setUrl("  https://x/img.png  ");
        request.setDescripcion("  Una obra  ");
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
        assertThat(guardada.getContenidoSensible()).isEqualTo(ContenidoSensible.DESNUDOS);
        assertThat(guardada.isEstado()).isTrue();
        assertThat(guardada.getFechaSubida()).isNotNull();
        assertThat(guardada.getPortfolio()).isSameAs(creado);
        assertThat(guardada.getUsuario()).isSameAs(artista);
        assertThat(guardada.getTipoArte()).isSameAs(tipoArte);

        assertThat(dto.getIdImagen()).isEqualTo(99L);
        assertThat(dto.getTitulo()).isEqualTo("Mi obra");
    }

    @Test
    void crear_conPortfolioExistente_loReutiliza() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio existente = portfolioDe(artista);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(existente));
        stubTipoArte();
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                eq(existente), eq("Mi obra"), eq(tipoArte))).thenReturn(false);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(99L);
            return imagen;
        });

        ImagenPortfolioRequest request = requestValido();

        ImagenPortfolioDTO dto = portfolioService.crear(7L, request);

        verify(portfolioRepository, never()).save(any(Portfolio.class));
        ArgumentCaptor<Imagen> imagenCaptor = ArgumentCaptor.forClass(Imagen.class);
        verify(imagenRepository).save(imagenCaptor.capture());
        assertThat(imagenCaptor.getValue().getPortfolio()).isSameAs(existente);
        assertThat(dto.getIdImagen()).isEqualTo(99L);
    }

    @Test
    void crear_tituloDuplicado_lanzaIllegalArgumentYNoGuarda() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio existente = portfolioDe(artista);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(existente));
        stubTipoArte();
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                eq(existente), eq("Mi obra"), eq(tipoArte))).thenReturn(true);

        assertThatThrownBy(() -> portfolioService.crear(7L, requestValido()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una imagen con ese título y tipo de arte en el portfolio.");

        verify(imagenRepository, never()).save(any(Imagen.class));
    }

    // ---------------------------------------------------------------
    // crear con etiquetas (Fase B)
    // ---------------------------------------------------------------

    @Test
    void crear_conEtiquetasValidas_guardaLasFilasDeUnion() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio existente = portfolioDe(artista);
        Etiqueta acuarela = etiquetaDe(1L, "Acuarela", EtiquetaEstado.VALIDA, null);
        Imagen[] guardada = new Imagen[1];
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(etiquetaRepository.findById(1L)).thenReturn(Optional.of(acuarela));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(existente));
        stubTipoArte();
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                eq(existente), eq("Mi obra"), eq(tipoArte))).thenReturn(false);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(99L);
            guardada[0] = imagen;
            return imagen;
        });
        when(imagenEtiquetaRepository.findByImagenIdImagen(99L))
                .thenAnswer(invocation -> List.of(fila(guardada[0], acuarela)));

        ImagenPortfolioRequest request = requestValido();
        request.setIdsEtiqueta(List.of(1L));

        ImagenPortfolioDTO dto = portfolioService.crear(7L, request);

        ArgumentCaptor<ImagenEtiqueta> filaCaptor = ArgumentCaptor.forClass(ImagenEtiqueta.class);
        verify(imagenEtiquetaRepository).save(filaCaptor.capture());
        assertThat(filaCaptor.getValue().getEtiqueta()).isSameAs(acuarela);
        assertThat(filaCaptor.getValue().getImagen().getIdImagen()).isEqualTo(99L);
        assertThat(dto.getEtiquetas()).extracting(EtiquetaDTO::getIdEtiqueta).containsExactly(1L);
    }

    @Test
    void crear_masDeCincoEtiquetas_lanzaErrorAntesDeEscribir() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));

        ImagenPortfolioRequest request = requestValido();
        request.setIdsEtiqueta(List.of(1L, 2L, 3L, 4L, 5L, 6L));

        assertThatThrownBy(() -> portfolioService.crear(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Una imagen puede tener como máximo 5 etiquetas.");

        verify(portfolioRepository, never()).findByArtista(any());
        verify(etiquetaRepository, never()).findById(any());
    }

    @Test
    void crear_etiquetaDadaDeBaja_lanzaError() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(etiquetaRepository.findById(1L))
                .thenReturn(Optional.of(etiquetaDe(1L, "Vieja", EtiquetaEstado.INACTIVA, null)));

        ImagenPortfolioRequest request = requestValido();
        request.setIdsEtiqueta(List.of(1L));

        assertThatThrownBy(() -> portfolioService.crear(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La etiqueta 'Vieja' fue dada de baja y no puede asignarse a piezas nuevas.");

        verify(portfolioRepository, never()).findByArtista(any());
    }

    @Test
    void crear_etiquetaRechazada_lanzaError() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(etiquetaRepository.findById(1L))
                .thenReturn(Optional.of(etiquetaDe(1L, "Manga", EtiquetaEstado.RECHAZADA, null)));

        ImagenPortfolioRequest request = requestValido();
        request.setIdsEtiqueta(List.of(1L));

        assertThatThrownBy(() -> portfolioService.crear(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La etiqueta 'Manga' fue rechazada y no puede asignarse a una imagen.");
    }

    @Test
    void crear_etiquetaPendienteAjena_lanzaError() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Usuario otro = usuarioConRol(30L, "otroArtista", ERol.ARTISTA);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(etiquetaRepository.findById(1L))
                .thenReturn(Optional.of(etiquetaDe(1L, "Pixel", EtiquetaEstado.PENDIENTE, otro)));

        ImagenPortfolioRequest request = requestValido();
        request.setIdsEtiqueta(List.of(1L));

        assertThatThrownBy(() -> portfolioService.crear(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La etiqueta 'Pixel' está pendiente de evaluación y solo su creador puede asignarla.");
    }

    @Test
    void crear_etiquetaPendientePropia_seAsigna() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio existente = portfolioDe(artista);
        Etiqueta propia = etiquetaDe(1L, "Pixel", EtiquetaEstado.PENDIENTE, artista);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(etiquetaRepository.findById(1L)).thenReturn(Optional.of(propia));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(existente));
        stubTipoArte();
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                eq(existente), eq("Mi obra"), eq(tipoArte))).thenReturn(false);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(99L);
            return imagen;
        });

        ImagenPortfolioRequest request = requestValido();
        request.setIdsEtiqueta(List.of(1L));

        portfolioService.crear(7L, request);

        ArgumentCaptor<ImagenEtiqueta> filaCaptor = ArgumentCaptor.forClass(ImagenEtiqueta.class);
        verify(imagenEtiquetaRepository).save(filaCaptor.capture());
        assertThat(filaCaptor.getValue().getEtiqueta()).isSameAs(propia);
    }

    @Test
    void crear_etiquetaInexistente_lanzaNoSuchElement() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(etiquetaRepository.findById(42L)).thenReturn(Optional.empty());

        ImagenPortfolioRequest request = requestValido();
        request.setIdsEtiqueta(List.of(42L));

        assertThatThrownBy(() -> portfolioService.crear(7L, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Etiqueta no encontrada: 42");

        verify(portfolioRepository, never()).findByArtista(any());
    }

    @Test
    void crear_etiquetasDuplicadas_lasDeduplica() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio existente = portfolioDe(artista);
        Etiqueta valida = etiquetaDe(1L, "Acuarela", EtiquetaEstado.VALIDA, null);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(etiquetaRepository.findById(1L)).thenReturn(Optional.of(valida));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(existente));
        stubTipoArte();
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                eq(existente), eq("Mi obra"), eq(tipoArte))).thenReturn(false);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(99L);
            return imagen;
        });

        ImagenPortfolioRequest request = requestValido();
        request.setIdsEtiqueta(List.of(1L, 1L));

        portfolioService.crear(7L, request);

        verify(etiquetaRepository).findById(1L);
        verify(imagenEtiquetaRepository).save(any(ImagenEtiqueta.class));
    }

    // ---------------------------------------------------------------
    // crear y modificar con materiales
    // ---------------------------------------------------------------

    @Test
    void crear_conMaterialesValidos_guardaLasFilasDeUnion() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio existente = portfolioDe(artista);
        Material procreate = materialDe(1L, "Procreate", medio(1L, "Digital"));
        Imagen[] guardada = new Imagen[1];
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(existente));
        stubTipoArte();
        when(materialRepository.findByIdMaterial(1L)).thenReturn(Optional.of(procreate));
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                eq(existente), eq("Mi obra"), eq(tipoArte))).thenReturn(false);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(99L);
            guardada[0] = imagen;
            return imagen;
        });
        when(imagenMaterialRepository.findByImagenIdImagen(99L))
                .thenAnswer(invocation -> List.of(filaMaterial(guardada[0], procreate)));

        ImagenPortfolioRequest request = requestValido();
        request.setIdsMaterial(List.of(1L));

        ImagenPortfolioDTO dto = portfolioService.crear(7L, request);

        ArgumentCaptor<ImagenMaterial> filaCaptor = ArgumentCaptor.forClass(ImagenMaterial.class);
        verify(imagenMaterialRepository).save(filaCaptor.capture());
        assertThat(filaCaptor.getValue().getMaterial()).isSameAs(procreate);
        assertThat(filaCaptor.getValue().getImagen().getIdImagen()).isEqualTo(99L);
        assertThat(dto.getMateriales()).extracting(MaterialDTO::getIdMaterial).containsExactly(1L);
    }

    @Test
    void crear_masDeCincoMateriales_lanzaErrorAntesDeEscribir() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        stubTipoArte();

        ImagenPortfolioRequest request = requestValido();
        request.setIdsMaterial(List.of(1L, 2L, 3L, 4L, 5L, 6L));

        assertThatThrownBy(() -> portfolioService.crear(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Una imagen puede tener como máximo 5 materiales.");

        verify(portfolioRepository, never()).findByArtista(any());
        verify(materialRepository, never()).findByIdMaterial(any());
    }

    @Test
    void crear_materialDadaDeBaja_lanzaError() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        stubTipoArte();
        Material viejo = materialDe(1L, "Viejo", medio(1L, "Digital"));
        viejo.setActivo(false);
        when(materialRepository.findByIdMaterial(1L)).thenReturn(Optional.of(viejo));

        ImagenPortfolioRequest request = requestValido();
        request.setIdsMaterial(List.of(1L));

        assertThatThrownBy(() -> portfolioService.crear(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El material 'Viejo' fue dado de baja y no puede asignarse a piezas nuevas.");

        verify(portfolioRepository, never()).findByArtista(any());
    }

    @Test
    void crear_materialDeOtroMedio_lanzaError() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        stubTipoArte();
        Material oleo = materialDe(1L, "Óleo", medio(2L, "Tradicional"));
        when(materialRepository.findByIdMaterial(1L)).thenReturn(Optional.of(oleo));

        ImagenPortfolioRequest request = requestValido();
        request.setIdsMaterial(List.of(1L));

        assertThatThrownBy(() -> portfolioService.crear(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El material 'Óleo' no corresponde al medio del tipo de arte seleccionado.");

        verify(portfolioRepository, never()).findByArtista(any());
    }

    @Test
    void modificar_reemplazaLosMateriales() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        Imagen imagen = imagenDe(portfolio, artista, "Obra 1", null);
        Material procreate = materialDe(1L, "Procreate", medio(1L, "Digital"));
        Material photoshop = materialDe(2L, "Photoshop", medio(1L, "Digital"));
        List<ImagenMaterial> filas = new ArrayList<>(List.of(filaMaterial(imagen, procreate)));

        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(imagenRepository.findById(5L)).thenReturn(Optional.of(imagen));
        when(imagenEtiquetaRepository.findByImagenIdImagen(5L)).thenReturn(List.of());
        when(imagenMaterialRepository.findByImagenIdImagen(5L))
                .thenAnswer(invocation -> List.copyOf(filas));
        stubTipoArte();
        when(materialRepository.findByIdMaterial(2L)).thenReturn(Optional.of(photoshop));
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                eq(portfolio), eq("Mi obra"), eq(tipoArte))).thenReturn(false);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imagenMaterialRepository.save(any(ImagenMaterial.class))).thenAnswer(invocation -> {
            ImagenMaterial nueva = invocation.getArgument(0);
            filas.add(nueva);
            return nueva;
        });
        doAnswer(invocation -> {
            filas.remove(invocation.getArgument(0));
            return null;
        }).when(imagenMaterialRepository).delete(any(ImagenMaterial.class));

        ImagenPortfolioRequest request = requestValido();
        request.setIdsMaterial(List.of(2L));

        ImagenPortfolioDTO dto = portfolioService.modificar(7L, 5L, request);

        verify(imagenMaterialRepository).delete(any(ImagenMaterial.class));
        verify(materialRepository, never()).findByIdMaterial(1L);
        assertThat(dto.getMateriales()).extracting(MaterialDTO::getNombre).containsExactly("Photoshop");
    }

    // ---------------------------------------------------------------
    // modificar (Fase B)
    // ---------------------------------------------------------------

    @Test
    void modificar_ok_actualizaCamposYReemplazaEtiquetas() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        Imagen imagen = imagenDe(portfolio, artista, "Obra 1", null);
        Etiqueta acuarela = etiquetaDe(1L, "Acuarela", EtiquetaEstado.VALIDA, null);
        Etiqueta pixel = etiquetaDe(2L, "Pixel", EtiquetaEstado.VALIDA, null);
        ImagenEtiqueta filaVieja = fila(imagen, acuarela);
        List<ImagenEtiqueta> filas = new ArrayList<>(List.of(filaVieja));

        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(imagenRepository.findById(5L)).thenReturn(Optional.of(imagen));
        when(imagenEtiquetaRepository.findByImagenIdImagen(5L)).thenAnswer(invocation -> List.copyOf(filas));
        when(etiquetaRepository.findById(2L)).thenReturn(Optional.of(pixel));
        stubTipoArte();
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                eq(portfolio), eq("Mi obra"), eq(tipoArte))).thenReturn(false);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imagenEtiquetaRepository.save(any(ImagenEtiqueta.class))).thenAnswer(invocation -> {
            ImagenEtiqueta nueva = invocation.getArgument(0);
            filas.add(nueva);
            return nueva;
        });
        doAnswer(invocation -> {
            filas.remove(invocation.getArgument(0));
            return null;
        }).when(imagenEtiquetaRepository).delete(any(ImagenEtiqueta.class));

        ImagenPortfolioRequest request = requestValido();
        request.setIdsEtiqueta(List.of(2L));

        ImagenPortfolioDTO dto = portfolioService.modificar(7L, 5L, request);

        verify(imagenEtiquetaRepository).delete(filaVieja);
        verify(etiquetaRepository, never()).findById(1L);
        assertThat(dto.getTitulo()).isEqualTo("Mi obra");
        assertThat(dto.getEtiquetas()).extracting(EtiquetaDTO::getNombre).containsExactly("Pixel");
    }

    @Test
    void modificar_conEtiquetaInactivaYaAsignada_laConserva() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        Imagen imagen = imagenDe(portfolio, artista, "Obra 1", null);
        Etiqueta inactiva = etiquetaDe(1L, "Vieja", EtiquetaEstado.INACTIVA, null);
        List<ImagenEtiqueta> filas = new ArrayList<>(List.of(fila(imagen, inactiva)));

        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(imagenRepository.findById(5L)).thenReturn(Optional.of(imagen));
        when(imagenEtiquetaRepository.findByImagenIdImagen(5L)).thenAnswer(invocation -> List.copyOf(filas));
        stubTipoArte();
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                eq(portfolio), eq("Mi obra"), eq(tipoArte))).thenReturn(false);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImagenPortfolioRequest request = requestValido();
        request.setIdsEtiqueta(List.of(1L));

        ImagenPortfolioDTO dto = portfolioService.modificar(7L, 5L, request);

        verify(imagenEtiquetaRepository, never()).delete(any(ImagenEtiqueta.class));
        verify(etiquetaRepository, never()).findById(any());
        assertThat(dto.getEtiquetas()).extracting(EtiquetaDTO::getNombre).containsExactly("Vieja");
    }

    @Test
    void modificar_imagenDeOtroArtista_lanzaNoSuchElement() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Usuario otro = usuarioArtista(8L, "OtroArtista");
        Imagen ajena = imagenDe(portfolioDe(otro), otro, "Obra ajena", null);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(imagenRepository.findById(5L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> portfolioService.modificar(7L, 5L, requestValido()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Imagen no encontrada: 5");

        verify(imagenRepository, never()).save(any(Imagen.class));
    }

    @Test
    void modificar_tituloRepetidoEnOtraImagen_lanzaError() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        Imagen imagen = imagenDe(portfolio, artista, "Obra 1", null);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(imagenRepository.findById(5L)).thenReturn(Optional.of(imagen));
        when(imagenEtiquetaRepository.findByImagenIdImagen(5L)).thenReturn(List.of());
        stubTipoArte();
        when(imagenRepository.existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                eq(portfolio), eq("Mi obra"), eq(tipoArte))).thenReturn(true);

        assertThatThrownBy(() -> portfolioService.modificar(7L, 5L, requestValido()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una imagen con ese título y tipo de arte en el portfolio.");

        verify(imagenRepository, never()).save(any(Imagen.class));
    }

    @Test
    void modificar_tituloYTipoSinCambiar_noConsultaUnicidad() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        Imagen imagen = imagenDe(portfolio, artista, "Obra 1", null);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(imagenRepository.findById(5L)).thenReturn(Optional.of(imagen));
        when(imagenEtiquetaRepository.findByImagenIdImagen(5L)).thenReturn(List.of());
        stubTipoArte();
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImagenPortfolioRequest request = requestValido();
        request.setTitulo("Obra 1");

        ImagenPortfolioDTO dto = portfolioService.modificar(7L, 5L, request);

        assertThat(dto.getTitulo()).isEqualTo("Obra 1");
        verify(imagenRepository, never())
                .existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(any(), any(), any());
    }

    @Test
    void modificar_artistaInexistente_lanzaNoSuchElement() {
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.modificar(99L, 5L, requestValido()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Artista no encontrado: 99");

        verify(imagenRepository, never()).findById(any());
    }

    @Test
    void modificar_seisEtiquetas_lanzaErrorAntesDeCargar() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));

        ImagenPortfolioRequest request = requestValido();
        request.setIdsEtiqueta(List.of(1L, 2L, 3L, 4L, 5L, 6L));

        assertThatThrownBy(() -> portfolioService.modificar(7L, 5L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Una imagen puede tener como máximo 5 etiquetas.");

        verify(imagenRepository, never()).findById(any());
    }

    @Test
    void modificar_imagenInexistente_lanzaNoSuchElement() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(imagenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.modificar(7L, 99L, requestValido()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Imagen no encontrada: 99");

        verify(imagenEtiquetaRepository, never()).findByImagenIdImagen(any());
    }

    // ---------------------------------------------------------------
    // darDeBaja (UC-13)
    // ---------------------------------------------------------------

    @Test
    void darDeBaja_ok_cambiaEstadoYBorraLasEtiquetas() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        Imagen imagen = imagenDe(portfolio, artista, "Obra 1", null);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(imagenRepository.findById(5L)).thenReturn(Optional.of(imagen));
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imagenEtiquetaRepository.findByImagenIdImagen(5L)).thenReturn(List.of());

        ImagenPortfolioDTO dto = portfolioService.darDeBaja(7L, 5L);

        ArgumentCaptor<Imagen> imagenCaptor = ArgumentCaptor.forClass(Imagen.class);
        verify(imagenRepository).save(imagenCaptor.capture());
        assertThat(imagenCaptor.getValue().isEstado()).isFalse();
        verify(imagenEtiquetaRepository).deleteByImagenIdImagen(5L);
        verify(imagenMaterialRepository).deleteByImagenIdImagen(5L);
        assertThat(dto.getIdImagen()).isEqualTo(5L);
    }

    @Test
    void darDeBaja_imagenDeOtroArtista_lanzaNoSuchElement() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Usuario otro = usuarioArtista(8L, "OtroArtista");
        Imagen ajena = imagenDe(portfolioDe(otro), otro, "Obra ajena", null);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(imagenRepository.findById(5L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> portfolioService.darDeBaja(7L, 5L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Imagen no encontrada: 5");

        verify(imagenRepository, never()).save(any(Imagen.class));
        verify(imagenEtiquetaRepository, never()).deleteByImagenIdImagen(any());
    }

    @Test
    void darDeBaja_imagenInexistente_lanzaNoSuchElement() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(imagenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.darDeBaja(7L, 99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Imagen no encontrada: 99");

        verify(imagenEtiquetaRepository, never()).deleteByImagenIdImagen(any());
    }

    @Test
    void darDeBaja_imagenYaDadaDeBaja_lanzaIllegalArgument() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Imagen imagen = imagenDe(portfolioDe(artista), artista, "Obra 1", null);
        imagen.setEstado(false);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(imagenRepository.findById(5L)).thenReturn(Optional.of(imagen));

        assertThatThrownBy(() -> portfolioService.darDeBaja(7L, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La imagen ya fue dada de baja.");

        verify(imagenRepository, never()).save(any(Imagen.class));
        verify(imagenEtiquetaRepository, never()).deleteByImagenIdImagen(any());
    }

    @Test
    void darDeBaja_artistaInexistente_lanzaNoSuchElement() {
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.darDeBaja(99L, 5L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Artista no encontrado: 99");

        verify(imagenRepository, never()).findById(any());
    }

    @Test
    void darDeBaja_usuarioNoArtista_lanzaIllegalArgument() {
        Usuario cliente = usuarioConRol(7L, "cliente42", ERol.CLIENTE);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> portfolioService.darDeBaja(7L, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El usuario no es un artista.");

        verify(imagenRepository, never()).findById(any());
    }

    // ---------------------------------------------------------------
    // listarParaVisor con filtros (búsqueda de portfolio)
    // ---------------------------------------------------------------

    @Test
    void listarParaVisor_conFiltros_losPasaALaQuery() {
        Imagen imagen = imagenDe(null, null, "Obra 1", null);
        FiltroPortfolioRequest filtro = new FiltroPortfolioRequest();
        filtro.setTitulo("  Obra  ");
        filtro.setIdEtiqueta(3L);
        filtro.setIdTipoArte(2L);
        filtro.setSensible("si");
        filtro.setOrden("antiguo");

        when(imagenRepository.findParaVisor(
                eq(10L), isNull(), eq("%obra%"), eq(3L), eq(2L),
                eq(Boolean.TRUE), eq(false), eq(true), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(List.of(imagen), PAGEABLE, 1));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, null, filtro, PAGEABLE);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getTitulo()).isEqualTo("Obra 1");
    }

    @Test
    void listarParaVisor_filtrosEnBlanco_seTratanComoSinFiltro() {
        FiltroPortfolioRequest filtro = new FiltroPortfolioRequest();
        filtro.setTitulo("");
        filtro.setSensible("");
        filtro.setOrden("");

        when(imagenRepository.findParaVisor(eq(10L), isNull(), isNull(), isNull(), isNull(),
                isNull(), eq(false), eq(false), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, null, filtro, PAGEABLE);

        assertThat(resultado.getContent()).isEmpty();
    }

    @Test
    void listarParaVisor_sensibleFalseTipoArteYOrdenReciente_loPasaCorrecto() {
        FiltroPortfolioRequest filtro = new FiltroPortfolioRequest();
        filtro.setIdTipoArte(1L);
        filtro.setSensible("no");
        filtro.setOrden("RECIENTE");

        when(imagenRepository.findParaVisor(eq(10L), isNull(), isNull(), isNull(),
                eq(1L), eq(Boolean.FALSE), eq(false), eq(false), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

        Page<ImagenPortfolioDTO> resultado = portfolioService
                .listarParaVisor(10L, null, filtro, PAGEABLE);

        assertThat(resultado.getContent()).isEmpty();
    }

    @Test
    void listarParaVisor_ordenInvalido_lanzaIllegalArgument() {
        FiltroPortfolioRequest filtro = new FiltroPortfolioRequest();
        filtro.setOrden("ALEATORIO");

        assertThatThrownBy(() -> portfolioService.listarParaVisor(10L, null, filtro, PAGEABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Orden no válido: ALEATORIO");

        verify(imagenRepository, never()).findParaVisor(any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void listarParaVisor_sensibleInvalido_lanzaIllegalArgument() {
        FiltroPortfolioRequest filtro = new FiltroPortfolioRequest();
        filtro.setSensible("quizas");

        assertThatThrownBy(() -> portfolioService.listarParaVisor(10L, null, filtro, PAGEABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filtro de contenido sensible no válido: quizas");
    }

    // ---------------------------------------------------------------
    // etiquetasDePortfolio
    // ---------------------------------------------------------------

    @Test
    void etiquetasDePortfolio_conPortfolio_devuelveEtiquetas() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Artista777"))
                .thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(portfolio));
        when(etiquetaRepository.findEtiquetasDePortfolio(10L))
                .thenReturn(List.of(etiquetaDe(1L, "Acuarela", EtiquetaEstado.VALIDA, null)));

        List<EtiquetaDTO> resultado = portfolioService.etiquetasDePortfolio("Artista777");

        assertThat(resultado).extracting(EtiquetaDTO::getNombre).containsExactly("Acuarela");
    }

    @Test
    void etiquetasDePortfolio_sinPortfolio_devuelveVacio() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Artista777"))
                .thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.empty());

        List<EtiquetaDTO> resultado = portfolioService.etiquetasDePortfolio("Artista777");

        assertThat(resultado).isEmpty();
        verify(etiquetaRepository, never()).findEtiquetasDePortfolio(any());
    }

    @Test
    void etiquetasDePortfolio_usuarioNoArtista_lanzaIllegalArgument() {
        Usuario cliente = usuarioConRol(7L, "cliente42", ERol.CLIENTE);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("cliente42"))
                .thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> portfolioService.etiquetasDePortfolio("cliente42"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El usuario no es un artista.");
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
    // crearDesdeEntrega (pieza verificada)
    // ---------------------------------------------------------------

    @Test
    void crearDesdeEntrega_ok_creaImagenVinculoYDevuelveVerificada() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        EntregaArchivo archivo = entregaFinal(artista, EstadoPedido.FINALIZADO);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(archivoRepository.buscarParaGaleria(23L)).thenReturn(Optional.of(archivo));
        when(verificacionRepository
                .existsByArchivoEntregaIdArchivoAndVigenteTrue(23L))
                .thenReturn(false);
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(portfolio));
        stubTipoArte();
        when(imagenRepository
                .existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                        eq(portfolio), eq("Mi obra"), eq(tipoArte)))
                .thenReturn(false);
        when(imagenRepository.save(any(Imagen.class))).thenAnswer(invocation -> {
            Imagen imagen = invocation.getArgument(0);
            imagen.setIdImagen(99L);
            return imagen;
        });
        when(imagenEtiquetaRepository.findByImagenIdImagen(99L)).thenReturn(List.of());
        when(imagenMaterialRepository.findByImagenIdImagen(99L)).thenReturn(List.of());
        PortfolioEntregaVerificada verificacion = new PortfolioEntregaVerificada();
        verificacion.setImagen(imagenDe(portfolio, artista, "Mi obra", null));
        verificacion.setArchivoEntrega(archivo);
        when(verificacionRepository.listarActivasPorImagenes(List.of(99L)))
                .thenReturn(List.of(verificacion));

        ImagenPortfolioDTO dto = portfolioService.crearDesdeEntrega(
            7L, requestVerificado(23L));

        ArgumentCaptor<Imagen> imagenCaptor = ArgumentCaptor.forClass(Imagen.class);
        verify(imagenRepository).save(imagenCaptor.capture());
        assertThat(imagenCaptor.getValue().getUrl())
                .isEqualTo("/api/portfolio/verificadas/archivos/23/preview");
        assertThat(imagenCaptor.getValue().getTitulo()).isEqualTo("Mi obra");
        verify(verificacionRepository).save(any(PortfolioEntregaVerificada.class));
        verify(imagenGaleriaService).asegurarLimpia(archivo);
        assertThat(dto.isVerificada()).isTrue();
        assertThat(dto.getServicioNombre()).isEqualTo("Ilustración");
    }

    @Test
    void crearDesdeEntrega_artistaAjeno_lanzaAccesoDenegado() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        EntregaArchivo archivo = entregaFinal(
            usuarioArtista(8L, "OtroArtista"), EstadoPedido.FINALIZADO);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(archivoRepository.buscarParaGaleria(23L)).thenReturn(Optional.of(archivo));

        assertThatThrownBy(() -> portfolioService.crearDesdeEntrega(
            7L, requestVerificado(23L)))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessage("No podés agregar esta entrega a tu portfolio.");

        verify(imagenRepository, never()).save(any(Imagen.class));
    }

    @Test
    void crearDesdeEntrega_pedidoNoFinalizado_lanzaIllegalArgument() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        EntregaArchivo archivo = entregaFinal(artista, EstadoPedido.EN_CURSO);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(archivoRepository.buscarParaGaleria(23L)).thenReturn(Optional.of(archivo));

        assertThatThrownBy(() -> portfolioService.crearDesdeEntrega(
            7L, requestVerificado(23L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El pedido todavía no está completo.");

        verify(imagenRepository, never()).save(any(Imagen.class));
    }

    @Test
    void crearDesdeEntrega_duplicada_lanzaIllegalArgument() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        EntregaArchivo archivo = entregaFinal(artista, EstadoPedido.FINALIZADO);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(archivoRepository.buscarParaGaleria(23L)).thenReturn(Optional.of(archivo));
        when(verificacionRepository
                .existsByArchivoEntregaIdArchivoAndVigenteTrue(23L))
                .thenReturn(true);

        assertThatThrownBy(() -> portfolioService.crearDesdeEntrega(
            7L, requestVerificado(23L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Esta entrega ya está en tu portfolio.");

        verify(imagenRepository, never()).save(any(Imagen.class));
    }

    @Test
    void modificar_piezaVerificadaConNuevaUrl_lanzaIllegalArgument() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        Imagen imagen = imagenDe(portfolio, artista, "Obra 1", null);
        when(usuarioRepository.findByIdUsuario(7L)).thenReturn(Optional.of(artista));
        when(imagenRepository.findById(5L)).thenReturn(Optional.of(imagen));
        when(imagenEtiquetaRepository.findByImagenIdImagen(5L)).thenReturn(List.of());
        when(imagenMaterialRepository.findByImagenIdImagen(5L)).thenReturn(List.of());
        stubTipoArte();
        when(imagenRepository
                .existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
                        eq(portfolio), eq("Mi obra"), eq(tipoArte)))
                .thenReturn(false);
        when(verificacionRepository
                .findByImagenIdImagenAndVigenteTrue(5L))
                .thenReturn(Optional.of(new PortfolioEntregaVerificada()));

        ImagenPortfolioRequest request = requestValido();
        request.setUrl("https://x/otra.png");

        assertThatThrownBy(() -> portfolioService.modificar(7L, 5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No podés reemplazar la imagen");

        verify(imagenRepository, never()).save(any(Imagen.class));
    }

    @Test
    void listar_marcaPiezasVerificadas() {
        Usuario artista = usuarioArtista(7L, "Artista777");
        Portfolio portfolio = portfolioDe(artista);
        Imagen imagen = imagenDe(portfolio, artista, "Obra 1", null);
        EntregaArchivo archivo = entregaFinal(artista, EstadoPedido.FINALIZADO);
        PortfolioEntregaVerificada verificacion = new PortfolioEntregaVerificada();
        verificacion.setImagen(imagen);
        verificacion.setArchivoEntrega(archivo);
        when(usuarioRepository.findByNombreUsuarioIgnoreCase("Artista777"))
                .thenReturn(Optional.of(artista));
        when(portfolioRepository.findByArtista(artista)).thenReturn(Optional.of(portfolio));
        when(imagenRepository.findByPortfolioAndEstadoTrueOrderByFechaSubidaDesc(portfolio))
                .thenReturn(List.of(imagen));
        when(imagenEtiquetaRepository.findByImagenIdImagenIn(List.of(5L)))
                .thenReturn(List.of());
        when(imagenMaterialRepository.findByImagenIdImagenIn(List.of(5L)))
                .thenReturn(List.of());
        when(verificacionRepository.listarActivasPorImagenes(List.of(5L)))
                .thenReturn(List.of(verificacion));

        List<ImagenPortfolioDTO> resultado = portfolioService.listar("Artista777");

        assertThat(resultado.get(0).isVerificada()).isTrue();
        assertThat(resultado.get(0).getServicioNombre()).isEqualTo("Ilustración");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private FiltroPortfolioRequest filtroVacio() {
        return new FiltroPortfolioRequest();
    }

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
        imagen.setTipoArte(tipoArte);
        imagen.setEstado(true);
        imagen.setContenidoSensible(contenidoSensible);
        imagen.setPortfolio(portfolio);
        imagen.setUsuario(autor);
        return imagen;
    }

    private ImagenPortfolioVerificadaRequest requestVerificado(Long idArchivo) {
        ImagenPortfolioVerificadaRequest request = new ImagenPortfolioVerificadaRequest();
        request.setIdArchivoEntrega(idArchivo);
        request.setTitulo("Mi obra");
        request.setDescripcion("Una obra");
        request.setIdTipoArte(1L);
        return request;
    }

    private EntregaArchivo entregaFinal(Usuario artista, EstadoPedido estado) {
        Usuario cliente = usuarioConRol(30L, "cliente30", ERol.CLIENTE);
        Servicio servicio = new Servicio();
        servicio.setIdServicio(4L);
        servicio.setNombre("Ilustración");

        Pedido pedido = new Pedido();
        pedido.setIdPedido(20L);
        pedido.setArtista(artista);
        pedido.setCliente(cliente);
        pedido.setServicio(servicio);
        pedido.setEstado(estado);

        HitoPedido hito = new HitoPedido();
        hito.setIdHitoPedido(21L);
        hito.setPedido(pedido);
        hito.setOrden(1);

        EntregaHito entrega = new EntregaHito();
        entrega.setIdEntrega(22L);
        entrega.setHitoPedido(hito);
        entrega.setVigente(true);
        entrega.setEsFinal(true);
        entrega.setRetirada(false);

        EntregaArchivo archivo = new EntregaArchivo();
        archivo.setIdArchivo(23L);
        archivo.setEntrega(entrega);
        archivo.setMime("image/png");
        archivo.setRetirado(false);
        archivo.setClaveOriginal("original/key.png");
        return archivo;
    }

    private ImagenPortfolioRequest requestValido() {
        ImagenPortfolioRequest request = new ImagenPortfolioRequest();
        request.setTitulo("Mi obra");
        request.setDescripcion("Una obra");
        request.setUrl("https://x/img.png");
        request.setIdTipoArte(1L);
        return request;
    }

    private Material materialDe(Long id, String nombre, Medio medio) {
        boolean digital = medio.getIdMedio() == 1L;
        return Material.builder()
                .idMaterial(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .tipoArte(TipoArte.builder()
                        .idTipoArte(digital ? 1L : 2L)
                        .nombre(digital ? "Digital" : "Tradicional")
                        .medio(medio)
                        .activo(true)
                        .build())
                .activo(true)
                .fechaCreacion(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    private Medio medio(Long id, String nombre) {
        return Medio.builder()
                .idMedio(id)
                .nombre(nombre)
                .activo(true)
                .build();
    }

    private ImagenMaterial filaMaterial(Imagen imagen, Material material) {
        return ImagenMaterial.builder()
                .imagen(imagen)
                .material(material)
                .build();
    }

    private Etiqueta etiquetaDe(Long id, String nombre, EtiquetaEstado estado, Usuario creador) {
        return Etiqueta.builder()
                .idEtiqueta(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .categoria(CategoriaEtiqueta.builder()
                        .idCategoriaEtiqueta(1L)
                        .nombre("Estilo")
                        .descripcion("Estilo artistico")
                        .activo(true)
                        .build())
                .creadoPor(creador)
                .estado(estado)
                .vecesSolicitada(0)
                .fechaCreacion(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    private ImagenEtiqueta fila(Imagen imagen, Etiqueta etiqueta) {
        return ImagenEtiqueta.builder()
                .imagen(imagen)
                .etiqueta(etiqueta)
                .build();
    }
}
