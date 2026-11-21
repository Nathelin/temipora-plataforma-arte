package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.InformeItemDTO;
import com.temipora.backend_springboot.modelo.InformeSerieDTO;
import com.temipora.backend_springboot.modelo.InformeTotalesDTO;
import com.temipora.backend_springboot.modelo.InformeVentasDTO;
import com.temipora.backend_springboot.repositorios.CompraGrupoRepository;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;

@ExtendWith(MockitoExtension.class)
class InformeVentasServiceTest {

    private static final Long ARTISTA = 2L;
    private static final String DESDE = "2026-11-01";
    private static final String HASTA = "2026-11-05";

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private CompraGrupoRepository compraGrupoRepository;
    @Mock
    private CompraItemRepository compraItemRepository;

    @InjectMocks
    private InformeVentasService informeVentasService;

    @Test
    void combinaServiciosYProductosYCalculaTotales() {
        stubServicios();
        stubProductos();

        InformeVentasDTO informe = informeVentasService.generar(
            ARTISTA, DESDE, HASTA, "TODOS", "DIA");

        assertThat(informe.periodo().desde()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(informe.periodo().hasta()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(informe.periodo().dias()).isEqualTo(5);
        assertThat(informe.moneda()).isEqualTo("ARS");

        InformeTotalesDTO totales = informe.totales();
        assertThat(totales.bruto()).isEqualByComparingTo("350.00");
        assertThat(totales.netoPercibido()).isEqualByComparingTo("315.00");
        assertThat(totales.costoCobro()).isEqualByComparingTo("35.00");
        assertThat(totales.comision()).isEqualByComparingTo("35.00");
        assertThat(totales.arancel()).isEqualByComparingTo("15.75");
        assertThat(totales.operaciones()).isEqualTo(2);
        assertThat(totales.unidades()).isEqualTo(4);
        assertThat(totales.ticketPromedio()).isEqualByComparingTo("157.50");
        assertThat(informe.operacionesSinNetoReal()).isZero();
    }

    @Test
    void reparteElNetoRealEntreLosItemsDelGrupo() {
        stubServicios();
        stubProductos();

        List<InformeItemDTO> items = informeVentasService.generar(
            ARTISTA, DESDE, HASTA, "TODOS", "DIA").items();

        assertThat(items).hasSize(3);
        assertThat(items.get(0).tipo()).isEqualTo("SERVICIO");
        assertThat(items.get(0).nombre()).isEqualTo("Ilustración");
        assertThat(items.get(0).netoPercibido()).isEqualByComparingTo("135.00");
        assertThat(items.get(0).unidades()).isEqualTo(2);
        assertThat(items.get(0).operaciones()).isEqualTo(1);
        assertThat(items.get(0).participacion()).isEqualByComparingTo("42.86");

        InformeItemDTO obraA = items.get(1);
        assertThat(obraA.id()).isEqualTo(30L);
        assertThat(obraA.netoPercibido()).isEqualByComparingTo("108.00");
        assertThat(obraA.arancel()).isEqualByComparingTo("5.40");
        assertThat(obraA.participacion()).isEqualByComparingTo("34.29");

        InformeItemDTO obraB = items.get(2);
        assertThat(obraB.id()).isEqualTo(31L);
        // El último ítem absorbe el residuo del prorrateo.
        assertThat(obraB.netoPercibido()).isEqualByComparingTo("72.00");
        assertThat(obraB.arancel()).isEqualByComparingTo("3.60");
    }

    @Test
    void completaLaSerieConLosPeriodosSinVentas() {
        stubServicios();
        stubProductos();

        List<InformeSerieDTO> serie = informeVentasService.generar(
            ARTISTA, DESDE, HASTA, "TODOS", "DIA").serie();

        assertThat(serie).hasSize(5);
        assertThat(serie.get(0).periodo()).isEqualTo("2026-11-01");
        assertThat(serie.get(0).netoPercibido()).isEqualByComparingTo("0.00");
        assertThat(serie.get(0).cobros()).isZero();
        assertThat(serie.get(1).periodo()).isEqualTo("2026-11-02");
        assertThat(serie.get(1).netoPercibido()).isEqualByComparingTo("135.00");
        assertThat(serie.get(1).cobros()).isEqualTo(2);
        assertThat(serie.get(2).netoPercibido()).isEqualByComparingTo("180.00");
        assertThat(serie.get(2).cobros()).isEqualTo(1);
    }

    @Test
    void agrupaLaSeriePorMes() {
        stubServicios();
        stubProductos();

        List<InformeSerieDTO> serie = informeVentasService.generar(
            ARTISTA, "2026-10-15", "2026-12-10", "TODOS", "MES").serie();

        assertThat(serie).extracting(InformeSerieDTO::periodo)
            .containsExactly("2026-10", "2026-11", "2026-12");
        assertThat(serie.get(1).netoPercibido()).isEqualByComparingTo("315.00");
    }

    @Test
    void usaElNetoDelArtistaCuandoFaltaElImporteLiquidado() {
        when(pagoRepository.filasInformeServicios(eq(ARTISTA), any(), any()))
            .thenReturn(List.<Object[]>of(new Object[] {
                LocalDate.of(2026, 11, 2), 7L, "Ilustración",
                new BigDecimal("100.00"), new BigDecimal("10.00"), null,
                new BigDecimal("90.00"), 1, 10L,
            }), List.of());

        InformeVentasDTO informe = informeVentasService.generar(
            ARTISTA, DESDE, HASTA, "SERVICIOS", "DIA");

        assertThat(informe.totales().netoPercibido())
            .isEqualByComparingTo("90.00");
        assertThat(informe.totales().arancel()).isEqualByComparingTo("0.00");
        assertThat(informe.operacionesSinNetoReal()).isEqualTo(1);
    }

    @Test
    void marcaEstimadoCuandoElGrupoNoTieneNetoLiquidado() {
        when(compraGrupoRepository.filasInformeGrupos(eq(ARTISTA), any(), any()))
            .thenReturn(List.<Object[]>of(new Object[] {
                LocalDate.of(2026, 11, 3), 5L, new BigDecimal("200.00"),
                new BigDecimal("20.00"), null, new BigDecimal("180.00"), 1, 1L,
            }), List.of());
        when(compraItemRepository.filasInformeProductos(
                eq(ARTISTA), any(), any()))
            .thenReturn(List.<Object[]>of(new Object[] {
                5L, 30L, "Obra A", 1, new BigDecimal("200.00"),
                new BigDecimal("20.00"),
            }));

        InformeVentasDTO informe = informeVentasService.generar(
            ARTISTA, DESDE, HASTA, "PRODUCTOS", "DIA");

        assertThat(informe.totales().netoPercibido())
            .isEqualByComparingTo("180.00");
        assertThat(informe.operacionesSinNetoReal()).isEqualTo(1);
        assertThat(informe.totales().operaciones()).isEqualTo(1);
        InformeItemDTO obra = informe.items().stream()
            .filter(item -> "Obra A".equals(item.nombre()))
            .findFirst()
            .orElseThrow();
        assertThat(obra.participacion()).isEqualByComparingTo("100.00");
        verify(pagoRepository, never())
            .filasInformeServicios(any(), any(), any());
    }

    @Test
    void comparaConElPeriodoAnteriorDeIgualDuracion() {
        stubServicios();
        stubProductos();

        InformeVentasDTO informe = informeVentasService.generar(
            ARTISTA, DESDE, HASTA, "TODOS", "DIA");

        assertThat(informe.comparacion().desde())
            .isEqualTo(LocalDate.of(2026, 10, 27));
        assertThat(informe.comparacion().hasta())
            .isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(informe.comparacion().netoPercibido())
            .isEqualByComparingTo("0.00");
        assertThat(informe.comparacion().variacionNetoPorcentaje()).isNull();
    }

    @Test
    void rechazaUnPeriodoInvertido() {
        assertThatThrownBy(() -> informeVentasService.generar(
            ARTISTA, "2026-11-10", "2026-11-01", null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("empezar antes de terminar");
    }

    @Test
    void rechazaUnaFechaInvalida() {
        assertThatThrownBy(() -> informeVentasService.generar(
            ARTISTA, "2026-13-01", "2026-11-01", null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Fecha inválida");
    }

    @Test
    void rechazaUnRangoDiarioDemasiadoLargo() {
        assertThatThrownBy(() -> informeVentasService.generar(
            ARTISTA, "2025-01-01", "2026-12-31", null, "DIA"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("agrupación diaria");
    }

    @Test
    void rechazaUnTipoInvalido() {
        assertThatThrownBy(() -> informeVentasService.generar(
            ARTISTA, DESDE, HASTA, "OTRO", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tipo de informe inválido");
    }

    @Test
    void usaElMesActualCuandoNoSeIndicaPeriodo() {
        stubServicios();
        stubProductos();

        InformeVentasDTO informe = informeVentasService.generar(
            ARTISTA, null, null, "TODOS", "DIA");

        LocalDate hoy = LocalDate.now();
        assertThat(informe.periodo().hasta()).isEqualTo(hoy);
        assertThat(informe.periodo().desde()).isEqualTo(hoy.minusDays(29));
        assertThat(informe.periodo().dias()).isEqualTo(30);
    }

    @Test
    void cuentaUnaSolaOperacionCuandoUnPedidoTieneVariosCobrosEstimados() {
        when(pagoRepository.filasInformeServicios(eq(ARTISTA), any(), any()))
            .thenReturn(List.<Object[]>of(
                new Object[] {
                    LocalDate.of(2026, 11, 2), 7L, "Ilustración",
                    new BigDecimal("100.00"), new BigDecimal("10.00"), null,
                    new BigDecimal("90.00"), 1, 10L,
                },
                new Object[] {
                    LocalDate.of(2026, 11, 3), 7L, "Ilustración",
                    new BigDecimal("50.00"), new BigDecimal("5.00"), null,
                    new BigDecimal("45.00"), 1, 10L,
                }), List.of());

        InformeVentasDTO informe = informeVentasService.generar(
            ARTISTA, DESDE, HASTA, "SERVICIOS", "DIA");

        assertThat(informe.totales().operaciones()).isEqualTo(1);
        assertThat(informe.totales().unidades()).isEqualTo(2);
        assertThat(informe.operacionesSinNetoReal()).isEqualTo(1);
    }

    private void stubServicios() {
        when(pagoRepository.filasInformeServicios(eq(ARTISTA), any(), any()))
            .thenReturn(List.<Object[]>of(
                new Object[] {
                    java.sql.Date.valueOf(LocalDate.of(2026, 11, 2)),
                    7L, "Ilustración", new BigDecimal("100.00"),
                    new BigDecimal("10.00"), new BigDecimal("4.50"),
                    new BigDecimal("85.50"), 0, 10L,
                },
                new Object[] {
                    LocalDate.of(2026, 11, 2),
                    7L, "Ilustración", new BigDecimal("50.00"),
                    new BigDecimal("5.00"), new BigDecimal("2.25"),
                    new BigDecimal("42.75"), 0, 10L,
                }), List.of());
    }

    private void stubProductos() {
        when(compraGrupoRepository.filasInformeGrupos(
                eq(ARTISTA), any(), any()))
            .thenReturn(List.<Object[]>of(new Object[] {
                LocalDate.of(2026, 11, 3), 5L, new BigDecimal("200.00"),
                new BigDecimal("20.00"), new BigDecimal("9.00"),
                new BigDecimal("171.00"), 0, 2L,
            }), List.of());
        when(compraItemRepository.filasInformeProductos(
                eq(ARTISTA), any(), any()))
            .thenReturn(List.<Object[]>of(
                new Object[] {
                    5L, 30L, "Obra A", 1, new BigDecimal("120.00"),
                    new BigDecimal("12.00"),
                },
                new Object[] {
                    5L, 31L, "Obra B", 1, new BigDecimal("80.00"),
                    new BigDecimal("8.00"),
                }));
    }
}
