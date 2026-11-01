package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.temipora.backend_springboot.modelo.CategoriaEtiqueta;
import com.temipora.backend_springboot.modelo.ConfiguracionRecomendacion;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioEtiqueta;
import com.temipora.backend_springboot.modelo.ServicioEtiquetaId;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.RecomendacionRepository;
import com.temipora.backend_springboot.repositorios.VisitaArtistaRepository;
import com.temipora.backend_springboot.repositorios.VisitaServicioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecomendacionServiceTest {

    @Mock
    private RecomendacionRepository recomendacionRepository;

    @Mock
    private VisitaArtistaRepository visitaArtistaRepository;

    @Mock
    private VisitaServicioRepository visitaServicioRepository;

    @Mock
    private ReseniaPublicaService reseniaPublicaService;

    @Mock
    private ConversionMonedaService conversionMonedaService;

    @Mock
    private ConfiguracionRecomendacionService configuracionService;

    @InjectMocks
    private RecomendacionService recomendacionService;

    private Moneda ars;

    @BeforeEach
    void setUp() {
        ars = Moneda.builder().idMoneda(1L).codigo("ARS").nombre("Peso").build();

        ConfiguracionRecomendacion config = new ConfiguracionRecomendacion();
        config.setMinFranjasPrecio(0);
        when(configuracionService.obtener()).thenReturn(config);

        when(conversionMonedaService.factor(any(), any(), any()))
            .thenReturn(BigDecimal.ONE);
        when(reseniaPublicaService.promediosPorServicios(anyCollection()))
            .thenReturn(Map.of());

        when(recomendacionRepository.artistasFavoritos(1L)).thenReturn(List.of());
        when(recomendacionRepository.serviciosFavoritos(1L)).thenReturn(List.of());
        when(recomendacionRepository.artistasComprados(1L)).thenReturn(List.of());
        when(recomendacionRepository.etiquetasCompradas(1L)).thenReturn(List.of());
        when(recomendacionRepository.etiquetasFavoritas(1L)).thenReturn(List.of());
        when(recomendacionRepository.coCompra(1L)).thenReturn(List.of());
        when(recomendacionRepository.ultimaCompraPorArtista(1L)).thenReturn(List.of());
        when(recomendacionRepository.ultimoFavoritoArtista(1L)).thenReturn(List.of());
        when(visitaServicioRepository.etiquetasVisitadas(eq(1L), any(LocalDate.class)))
            .thenReturn(List.of());
        when(visitaArtistaRepository.coVisitas(eq(1L), any(LocalDate.class)))
            .thenReturn(List.of());
        when(visitaArtistaRepository.resumenPorUsuario(eq(1L), any(LocalDate.class)))
            .thenReturn(List.of());
    }

    @Test
    void rankear_sinUsuario_mantieneOrdenAlfabeticoYSinPersonalizar() {
        Servicio b = servicio(2L, "Boceto", 20L, "200");
        Servicio a = servicio(1L, "Acuarela", 10L, "100");

        RecomendacionService.ResultadoRanking resultado =
            recomendacionService.rankear(List.of(b, a), null, ars);

        assertThat(resultado.personalizado()).isFalse();
        assertThat(resultado.items())
            .extracting(item -> item.servicio().getNombre())
            .containsExactly("Acuarela", "Boceto");
    }

    @Test
    void rankear_sinSenales_noPersonaliza() {
        Servicio a = servicio(1L, "Acuarela", 10L, "100");

        RecomendacionService.ResultadoRanking resultado =
            recomendacionService.rankear(List.of(a), 1L, ars);

        assertThat(resultado.personalizado()).isFalse();
        assertThat(resultado.items()).hasSize(1);
    }

    @Test
    void rankear_priorizaElArtistaFavorito() {
        when(recomendacionRepository.artistasFavoritos(1L)).thenReturn(List.of(10L));

        Servicio noFavorito = servicio(2L, "Boceto", 20L, "200");
        Servicio favorito = servicio(1L, "Acuarela", 10L, "100");

        RecomendacionService.ResultadoRanking resultado =
            recomendacionService.rankear(List.of(noFavorito, favorito), 1L, ars);

        assertThat(resultado.personalizado()).isTrue();
        assertThat(resultado.items()).hasSize(2);
        assertThat(resultado.items().get(0).servicio().getIdServicio()).isEqualTo(1L);
        assertThat(resultado.items().get(0).motivo()).isEqualTo("Artista que seguís");
    }

    @Test
    void similares_excluyeElMismoArtistaYPrefiereEtiquetasIguales() {
        CategoriaEtiqueta categoria = CategoriaEtiqueta.builder()
            .idCategoriaEtiqueta(1L)
            .nombre("Estilo")
            .build();
        Etiqueta etiqueta = Etiqueta.builder()
            .idEtiqueta(5L)
            .nombre("Anime")
            .categoria(categoria)
            .build();

        Servicio origen = servicio(1L, "Origen", 99L, "100");
        origen.setEtiquetas(etiquetas(etiqueta));

        Servicio mismoArtista = servicio(2L, "Mismo artista", 99L, "100");
        mismoArtista.setEtiquetas(etiquetas(etiqueta));

        Servicio similar = servicio(3L, "Similar", 20L, "110");
        similar.setEtiquetas(etiquetas(etiqueta));

        Servicio distinto = servicio(4L, "Distinto", 30L, "110");

        List<RecomendacionService.ServicioPuntuado> resultado =
            recomendacionService.similares(
                origen,
                List.of(mismoArtista, distinto, similar),
                null,
                ars);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).servicio().getIdServicio()).isEqualTo(3L);
        assertThat(resultado.get(0).motivo()).isEqualTo("Mismo estilo");
        assertThat(resultado)
            .extracting(item -> item.servicio().getIdServicio())
            .doesNotContain(2L);
    }

    private Servicio servicio(Long id, String nombre, Long idArtista, String precio) {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(id);
        servicio.setNombre(nombre);
        servicio.setPrecioBase(new BigDecimal(precio));
        servicio.setMoneda(ars);
        servicio.setCupos(3);
        servicio.setEtiquetas(new LinkedHashSet<>());
        Usuario artista = Usuario.builder()
            .idUsuario(idArtista)
            .nombreUsuario("artista" + idArtista)
            .build();
        servicio.setArtista(artista);
        return servicio;
    }

    private Set<ServicioEtiqueta> etiquetas(Etiqueta etiqueta) {
        ServicioEtiqueta asignacion = new ServicioEtiqueta();
        asignacion.setId(new ServicioEtiquetaId());
        asignacion.setEtiqueta(etiqueta);
        Set<ServicioEtiqueta> etiquetas = new LinkedHashSet<>();
        etiquetas.add(asignacion);
        return etiquetas;
    }
}
