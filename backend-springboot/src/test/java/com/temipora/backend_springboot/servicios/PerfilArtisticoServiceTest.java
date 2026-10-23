package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.Imagen;
import com.temipora.backend_springboot.modelo.ImagenEtiqueta;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.PerfilArtisticoDTO;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioEtiqueta;
import com.temipora.backend_springboot.modelo.ServicioEtiquetaId;
import com.temipora.backend_springboot.modelo.TipoArte;
import com.temipora.backend_springboot.modelo.TipoServicio;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ImagenEtiquetaRepository;
import com.temipora.backend_springboot.repositorios.ImagenRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;

@ExtendWith(MockitoExtension.class)
class PerfilArtisticoServiceTest {

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private ImagenEtiquetaRepository imagenEtiquetaRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @InjectMocks
    private PerfilArtisticoService perfilArtisticoService;

    @Test
    void calcular_agregaTiposMediosEtiquetasYServicios() {
        Usuario artista = artista(2L);
        Medio digital = medio(1L, "Digital");
        Medio tradicional = medio(2L, "Tradicional");
        TipoArte acuarela = tipoArte(10L, "Acuarela", digital);
        TipoArte oleo = tipoArte(11L, "Óleo", tradicional);

        Imagen primera = imagen(1L, artista, acuarela);
        Imagen segunda = imagen(2L, artista, acuarela);
        Imagen tercera = imagen(3L, artista, oleo);

        Etiqueta retrato = etiqueta(1L, "Retrato");
        Etiqueta paisaje = etiqueta(2L, "Paisaje");
        Etiqueta bosquejo = etiqueta(3L, "Bosquejo");

        TipoServicio comision = tipoServicio(1L, "Comisión");
        TipoServicio ilustracion = tipoServicio(2L, "Ilustración");

        Servicio servicio1 = servicio(20L, artista, comision, digital, List.of(retrato));
        Servicio servicio2 = servicio(21L, artista, comision, digital, List.of(paisaje));
        Servicio servicio3 = servicio(22L, artista, ilustracion, digital, List.of(bosquejo));

        when(imagenRepository.findByUsuarioIdUsuarioInAndEstadoTrue(List.of(2L)))
            .thenReturn(List.of(primera, segunda, tercera));
        when(servicioRepository.findByArtistaIdUsuarioInAndEstadoTrue(List.of(2L)))
            .thenReturn(List.of(servicio1, servicio2, servicio3));
        when(imagenEtiquetaRepository.findByImagenIdImagenIn(List.of(1L, 2L, 3L)))
            .thenReturn(List.of(
                etiquetaDeImagen(primera, retrato),
                etiquetaDeImagen(segunda, retrato),
                etiquetaDeImagen(tercera, paisaje)));

        PerfilArtisticoDTO resultado = perfilArtisticoService.calcular(2L);

        assertThat(resultado.getTipoArtePrincipal()).isEqualTo("Acuarela");
        assertThat(resultado.getMedioPrincipal()).isEqualTo("Digital");
        assertThat(resultado.getEtiquetasDestacadas())
            .containsExactly("Retrato", "Paisaje", "Bosquejo");
        assertThat(resultado.getTiposServicioDestacados())
            .containsExactly("Comisión", "Ilustración");
        assertThat(resultado.getCantidadImagenes()).isEqualTo(3);
        assertThat(resultado.getCantidadServicios()).isEqualTo(3);
    }

    @Test
    void calcular_sinDatos_devuelvePerfilVacio() {
        when(imagenRepository.findByUsuarioIdUsuarioInAndEstadoTrue(List.of(2L)))
            .thenReturn(List.of());
        when(servicioRepository.findByArtistaIdUsuarioInAndEstadoTrue(List.of(2L)))
            .thenReturn(List.of());

        PerfilArtisticoDTO resultado = perfilArtisticoService.calcular(2L);

        assertThat(resultado.getTipoArtePrincipal()).isNull();
        assertThat(resultado.getMedioPrincipal()).isNull();
        assertThat(resultado.getEtiquetasDestacadas()).isEmpty();
        assertThat(resultado.getTiposServicioDestacados()).isEmpty();
        assertThat(resultado.getCantidadImagenes()).isZero();
        assertThat(resultado.getCantidadServicios()).isZero();
        verify(imagenEtiquetaRepository, never()).findByImagenIdImagenIn(any());
    }

    @Test
    void calcular_idNulo_devuelvePerfilVacio() {
        PerfilArtisticoDTO resultado = perfilArtisticoService.calcular(null);

        assertThat(resultado.getTipoArtePrincipal()).isNull();
        assertThat(resultado.getEtiquetasDestacadas()).isEmpty();
        verifyNoInteractions(imagenRepository, imagenEtiquetaRepository, servicioRepository);
    }

    @Test
    void calcularParaArtistas_sinIds_devuelveMapaVacio() {
        Map<Long, PerfilArtisticoDTO> resultado = perfilArtisticoService
            .calcularParaArtistas(List.of());

        assertThat(resultado).isEmpty();
        verifyNoInteractions(imagenRepository, imagenEtiquetaRepository, servicioRepository);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Usuario artista(Long id) {
        return Usuario.builder()
            .idUsuario(id)
            .nombreUsuario("artista" + id)
            .build();
    }

    private Medio medio(Long id, String nombre) {
        return Medio.builder()
            .idMedio(id)
            .nombre(nombre)
            .build();
    }

    private TipoArte tipoArte(Long id, String nombre, Medio medio) {
        return TipoArte.builder()
            .idTipoArte(id)
            .nombre(nombre)
            .medio(medio)
            .build();
    }

    private Imagen imagen(Long id, Usuario artista, TipoArte tipoArte) {
        Imagen imagen = new Imagen();
        imagen.setIdImagen(id);
        imagen.setEstado(true);
        imagen.setUrl("imagen" + id + ".png");
        imagen.setTitulo("Imagen " + id);
        imagen.setDescripcion("desc");
        imagen.setTipoArte(tipoArte);
        imagen.setUsuario(artista);
        return imagen;
    }

    private Etiqueta etiqueta(Long id, String nombre) {
        return Etiqueta.builder()
            .idEtiqueta(id)
            .nombre(nombre)
            .build();
    }

    private ImagenEtiqueta etiquetaDeImagen(Imagen imagen, Etiqueta etiqueta) {
        return ImagenEtiqueta.builder()
            .imagen(imagen)
            .etiqueta(etiqueta)
            .build();
    }

    private TipoServicio tipoServicio(Long id, String nombre) {
        return TipoServicio.builder()
            .idTipoServicio(id)
            .nombre(nombre)
            .build();
    }

    private Servicio servicio(
            Long id,
            Usuario artista,
            TipoServicio tipoServicio,
            Medio medio,
            List<Etiqueta> etiquetas) {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(id);
        servicio.setArtista(artista);
        servicio.setTipoServicio(tipoServicio);
        servicio.setMedio(medio);
        servicio.setNombre("Servicio " + id);
        servicio.setDescripcion("desc");
        servicio.setPrecioBase(BigDecimal.TEN);
        servicio.setEstado(true);

        for (Etiqueta etiqueta : etiquetas) {
            ServicioEtiqueta asignacion = new ServicioEtiqueta();
            asignacion.setId(new ServicioEtiquetaId(id, etiqueta.getIdEtiqueta()));
            asignacion.setServicio(servicio);
            asignacion.setEtiqueta(etiqueta);
            servicio.getEtiquetas().add(asignacion);
        }
        return servicio;
    }
}
