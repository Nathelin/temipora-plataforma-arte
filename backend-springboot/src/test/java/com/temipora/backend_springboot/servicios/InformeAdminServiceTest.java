package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.InformeAdminDTO;
import com.temipora.backend_springboot.modelo.InformeAdminItemDTO;
import com.temipora.backend_springboot.modelo.InformeAdminSerieDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTipoDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTotalesDTO;
import com.temipora.backend_springboot.modelo.InformeAdminVendedorDTO;
import com.temipora.backend_springboot.repositorios.CompraGrupoRepository;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;

@ExtendWith(MockitoExtension.class)
class InformeAdminServiceTest {

    private static final String DESDE = "2026-11-01";
    private static final String HASTA = "2026-11-05";

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private CompraGrupoRepository compraGrupoRepository;
    @Mock
    private CompraItemRepository compraItemRepository;

    @InjectMocks
    private InformeAdminService informeAdminService;

    @Test
    void combinaServiciosYProductosYCalculaTotales() {
        stubTodo();

        InformeAdminDTO informe = informeAdminService.generar(
            DESDE, HASTA, "TODOS", "DIA");

        InformeAdminTotalesDTO totales = informe.totales();
        assertThat(totales.volumen()).isEqualByComparingTo("350.00");
        assertThat(totales.comision()).isEqualByComparingTo("35.00");
        assertThat(totales.netoArtistas()).isEqualByComparingTo("315.00");
        assertThat(totales.arancel()).isEqualByComparingTo("15.75");
        assertThat(totales.netoPercibido()).isEqualByComparingTo("299.25");
        assertThat(totales.operaciones()).isEqualTo(2);
        assertThat(totales.unidades()).isEqualTo(4);
        assertThat(totales.ticketPromedio()).isEqualByComparingTo("175.00");
        assertThat(informe.operacionesSinNetoReal()).isZero();
    }

    @Test
    void calculaCortesPorTipoVendedorEItem() {
        stubTodo();

        InformeAdminDTO informe = informeAdminService.generar(
            DESDE, HASTA, "TODOS", "DIA");

        List<InformeAdminTipoDTO> tipos = informe.tipos();
        assertThat(tipos).extracting(InformeAdminTipoDTO::tipo)
            .containsExactly("PRODUCTO", "SERVICIO");

        InformeAdminTipoDTO servicio = tipos.stream()
            .filter(tipo -> "SERVICIO".equals(tipo.tipo()))
            .findFirst().orElseThrow();
        assertThat(servicio.operaciones()).isEqualTo(1);
        assertThat(servicio.unidades()).isEqualTo(2);
        assertThat(servicio.volumen()).isEqualByComparingTo("150.00");
        assertThat(servicio.comision()).isEqualByComparingTo("15.00");

        InformeAdminTipoDTO producto = tipos.stream()
            .filter(tipo -> "PRODUCTO".equals(tipo.tipo()))
            .findFirst().orElseThrow();
        assertThat(producto.operaciones()).isEqualTo(1);
        assertThat(producto.unidades()).isEqualTo(2);
        assertThat(producto.volumen()).isEqualByComparingTo("200.00");
        assertThat(producto.comision()).isEqualByComparingTo("20.00");

        List<InformeAdminVendedorDTO> vendedores = informe.vendedores();
        assertThat(vendedores).hasSize(2);
        assertThat(vendedores.get(0).nombre()).isEqualTo("artista2");
        assertThat(vendedores.get(0).comision()).isEqualByComparingTo("20.00");
        assertThat(vendedores.get(1).nombre()).isEqualTo("artista1");
        assertThat(vendedores.get(1).volumen()).isEqualByComparingTo("150.00");

        List<InformeAdminItemDTO> items = informe.items();
        assertThat(items).extracting(InformeAdminItemDTO::nombre)
            .containsExactly("Ilustración", "Obra A", "Obra B");
        InformeAdminItemDTO obraA = items.get(1);
        assertThat(obraA.netoArtistas()).isEqualByComparingTo("108.00");
        assertThat(obraA.arancel()).isEqualByComparingTo("5.40");
        InformeAdminItemDTO obraB = items.get(2);
        assertThat(obraB.netoArtistas()).isEqualByComparingTo("72.00");
        assertThat(obraB.arancel()).isEqualByComparingTo("3.60");
    }

    @Test
    void completaLaSerieYComparaConElPeriodoAnterior() {
        stubTodo();

        InformeAdminDTO informe = informeAdminService.generar(
            DESDE, HASTA, "TODOS", "DIA");

        List<InformeAdminSerieDTO> serie = informe.serie();
        assertThat(serie).hasSize(5);
        assertThat(serie.get(0).volumen()).isEqualByComparingTo("0.00");
        assertThat(serie.get(1).volumen()).isEqualByComparingTo("150.00");
        assertThat(serie.get(1).comision()).isEqualByComparingTo("15.00");
        assertThat(serie.get(1).cobros()).isEqualTo(2);
        assertThat(serie.get(2).volumen()).isEqualByComparingTo("200.00");
        assertThat(serie.get(2).cobros()).isEqualTo(1);

        assertThat(informe.comparacion().desde())
            .isEqualTo(LocalDate.of(2026, 10, 27));
        assertThat(informe.comparacion().hasta())
            .isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(informe.comparacion().variacionVolumenPorcentaje()).isNull();
        assertThat(informe.comparacion().variacionComisionPorcentaje()).isNull();
    }

    @Test
    void marcaOperacionesSinNetoReal() {
        when(pagoRepository.filasInformeAdminServicios(any(), any()))
            .thenReturn(List.<Object[]>of(new Object[] {
                LocalDate.of(2026, 11, 2), 2L, "artista1", 7L,
                "Ilustración", new BigDecimal("100.00"),
                new BigDecimal("10.00"), new BigDecimal("90.00"), null,
                new BigDecimal("90.00"), 1, 10L,
            }), List.of());
        when(compraGrupoRepository.filasInformeAdminGrupos(any(), any()))
            .thenReturn(List.<Object[]>of(new Object[] {
                LocalDate.of(2026, 11, 3), 3L, "artista2",
                new BigDecimal("200.00"), new BigDecimal("20.00"),
                new BigDecimal("180.00"), null, new BigDecimal("180.00"),
                1, 1L, 5L,
            }), List.of());
        when(compraItemRepository.filasInformeAdminProductos(any(), any()))
            .thenReturn(List.<Object[]>of(new Object[] {
                5L, 30L, "Obra A", 1, new BigDecimal("200.00"),
                new BigDecimal("20.00"),
            }));

        InformeAdminDTO informe = informeAdminService.generar(
            DESDE, HASTA, "TODOS", "DIA");

        assertThat(informe.operacionesSinNetoReal()).isEqualTo(2);
        assertThat(informe.totales().netoPercibido())
            .isEqualByComparingTo("270.00");
    }

    @Test
    void filtraSoloProductos() {
        when(compraGrupoRepository.filasInformeAdminGrupos(any(), any()))
            .thenReturn(List.<Object[]>of(new Object[] {
                LocalDate.of(2026, 11, 3), 3L, "artista2",
                new BigDecimal("200.00"), new BigDecimal("20.00"),
                new BigDecimal("180.00"), new BigDecimal("9.00"),
                new BigDecimal("171.00"), 0, 2L, 5L,
            }), List.of());
        when(compraItemRepository.filasInformeAdminProductos(any(), any()))
            .thenReturn(List.<Object[]>of(new Object[] {
                5L, 30L, "Obra A", 2, new BigDecimal("100.00"),
                new BigDecimal("10.00"),
            }));

        InformeAdminDTO informe = informeAdminService.generar(
            DESDE, HASTA, "PRODUCTOS", "DIA");

        assertThat(informe.totales().volumen())
            .isEqualByComparingTo("200.00");
        assertThat(informe.tipos()).hasSize(1);
        assertThat(informe.tipos().get(0).tipo()).isEqualTo("PRODUCTO");
        verify(pagoRepository, never())
            .filasInformeAdminServicios(any(), any());
    }

    @Test
    void rechazaUnPeriodoInvertido() {
        assertThatThrownBy(() -> informeAdminService.generar(
            "2026-11-10", "2026-11-01", null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("empezar antes de terminar");
    }

    @Test
    void rechazaUnaFechaInvalida() {
        assertThatThrownBy(() -> informeAdminService.generar(
            "2026-13-01", "2026-11-01", null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Fecha inválida");
    }

    @Test
    void rechazaUnRangoDiarioDemasiadoLargo() {
        assertThatThrownBy(() -> informeAdminService.generar(
            "2025-01-01", "2026-12-31", null, "DIA"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("agrupación diaria");
    }

    private void stubTodo() {
        when(pagoRepository.filasInformeAdminServicios(any(), any()))
            .thenReturn(List.<Object[]>of(
                new Object[] {
                    java.sql.Date.valueOf(LocalDate.of(2026, 11, 2)),
                    2L, "artista1", 7L, "Ilustración",
                    new BigDecimal("100.00"), new BigDecimal("10.00"),
                    new BigDecimal("90.00"), new BigDecimal("4.50"),
                    new BigDecimal("85.50"), 0, 10L,
                },
                new Object[] {
                    LocalDate.of(2026, 11, 2),
                    2L, "artista1", 7L, "Ilustración",
                    new BigDecimal("50.00"), new BigDecimal("5.00"),
                    new BigDecimal("45.00"), new BigDecimal("2.25"),
                    new BigDecimal("42.75"), 0, 10L,
                }), List.of());
        when(compraGrupoRepository.filasInformeAdminGrupos(any(), any()))
            .thenReturn(List.<Object[]>of(new Object[] {
                LocalDate.of(2026, 11, 3), 3L, "artista2",
                new BigDecimal("200.00"), new BigDecimal("20.00"),
                new BigDecimal("180.00"), new BigDecimal("9.00"),
                new BigDecimal("171.00"), 0, 2L, 5L,
            }), List.of());
        when(compraItemRepository.filasInformeAdminProductos(any(), any()))
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
