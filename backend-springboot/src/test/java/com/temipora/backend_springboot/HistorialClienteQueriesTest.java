package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.temipora.backend_springboot.modelo.HistorialClienteDTO;
import com.temipora.backend_springboot.servicios.HistorialClienteService;

import jakarta.persistence.EntityManager;

@SpringBootTest
class HistorialClienteQueriesTest {

    @Autowired
    private HistorialClienteService service;

    @Autowired
    private EntityManager entityManager;

    @Test
    void consultasDelHistorial_ejecutanSinErrores() {
        Long idCliente = primerClienteConHistorial();
        assumeTrue(idCliente != null, "No hay pedidos ni compras para probar");

        HistorialClienteDTO historial = service.listar(
            idCliente, null, null, "TODOS", "TODOS", null, 0, 20);

        assertThat(historial.resumen()).isNotNull();
        assertThat(historial.contenido()).isNotNull();
        assertThat(historial.totalElementos())
            .isGreaterThanOrEqualTo(0L);
        assertThat(historial.periodoTexto()).isEqualTo("Todo el historial");
        assertThat(historial.resumen().moneda()).isEqualTo("ARS");

        if (!historial.contenido().isEmpty()) {
            var primero = historial.contenido().get(0);
            assertThat(primero.tipo()).isIn("PEDIDO", "COMPRA");
            assertThat(primero.id()).isNotNull();
            assertThat(primero.fecha()).isNotNull();
        }
    }

    @Test
    void filtroCompletados_soloDevuelveFinalizados() {
        Long idCliente = primerClienteConHistorial();
        assumeTrue(idCliente != null, "No hay pedidos ni compras para probar");

        HistorialClienteDTO historial = service.listar(
            idCliente, null, null, "TODOS", "FINALIZADO", null, 0, 20);

        assertThat(historial.contenido())
            .allMatch(item -> "FINALIZADO".equals(item.estadoGrupo()));
    }

    @Test
    void filtroPorTipo_devuelveSoloEseTipo() {
        Long idCliente = primerClienteConHistorial();
        assumeTrue(idCliente != null, "No hay pedidos ni compras para probar");

        HistorialClienteDTO historial = service.listar(
            idCliente, null, null, "PEDIDO", "TODOS", null, 0, 20);

        assertThat(historial.contenido())
            .allMatch(item -> "PEDIDO".equals(item.tipo()));
    }

    @Test
    void filtroAbiertos_soloDevuelveEnCursoOPendientes() {
        Long idCliente = primerClienteConHistorial();
        assumeTrue(idCliente != null, "No hay pedidos ni compras para probar");

        HistorialClienteDTO historial = service.listar(
            idCliente, null, null, "TODOS", "ABIERTO", null, 0, 20);

        assertThat(historial.contenido()).allMatch(item ->
            "EN_CURSO".equals(item.estadoGrupo())
                || "PENDIENTE".equals(item.estadoGrupo()));
    }

    @Test
    void busqueda_filtraPorTituloOContraparte() {
        Long idCliente = primerClienteConHistorial();
        assumeTrue(idCliente != null, "No hay pedidos ni compras para probar");

        HistorialClienteDTO historial = service.listar(
            idCliente, null, null, "TODOS", "TODOS", "artista", 0, 20);

        assertThat(historial.contenido()).allMatch(item ->
            item.titulo().toLowerCase().contains("artista")
                || item.contraparte().toLowerCase().contains("artista"));
    }

    @Test
    void filtroCancelados_soloDevuelveCancelados() {
        Long idCliente = primerClienteConHistorial();
        assumeTrue(idCliente != null, "No hay pedidos ni compras para probar");

        HistorialClienteDTO historial = service.listar(
            idCliente, null, null, "TODOS", "CANCELADO", null, 0, 20);

        assertThat(historial.contenido())
            .allMatch(item -> "CANCELADO".equals(item.estadoGrupo()));
    }

    @Test
    void filtroCompras_soloDevuelveCompras() {
        Long idCliente = primerClienteConHistorial();
        assumeTrue(idCliente != null, "No hay pedidos ni compras para probar");

        HistorialClienteDTO historial = service.listar(
            idCliente, null, null, "COMPRA", "TODOS", null, 0, 20);

        assertThat(historial.contenido())
            .allMatch(item -> "COMPRA".equals(item.tipo()));
    }

    @Test
    void paginacion_devuelvePaginasDistintas() {
        Long idCliente = primerClienteConHistorial();
        assumeTrue(idCliente != null, "No hay pedidos ni compras para probar");

        HistorialClienteDTO completo = service.listar(
            idCliente, null, null, "TODOS", "TODOS", null, 0, 20);
        assumeTrue(completo.totalElementos() > 1,
            "Hace falta más de una operación para probar la paginación");

        HistorialClienteDTO primera = service.listar(
            idCliente, null, null, "TODOS", "TODOS", null, 0, 1);
        HistorialClienteDTO segunda = service.listar(
            idCliente, null, null, "TODOS", "TODOS", null, 1, 1);

        assertThat(primera.contenido()).hasSize(1);
        assertThat(segunda.contenido()).hasSize(1);
        assertThat(primera.totalPaginas()).isGreaterThanOrEqualTo(2);
        assertThat(primera.contenido().get(0).id())
            .isNotEqualTo(segunda.contenido().get(0).id());
    }

    @Test
    void filtrosInvalidos_fallan() {
        Long idCliente = primerClienteConHistorial();
        assumeTrue(idCliente != null, "No hay pedidos ni compras para probar");

        assertThatThrownBy(() -> service.listar(
            idCliente, null, null, "OTRO", "TODOS", null, 0, 20))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listar(
            idCliente, null, null, "TODOS", "OTRO", null, 0, 20))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @SuppressWarnings("unchecked")
    private Long primerClienteConHistorial() {
        List<Object> ids = entityManager.createNativeQuery("""
            select id_cliente from pedido
            union
            select id_cliente from compra
            """).getResultList();
        if (ids.isEmpty()) {
            return null;
        }
        Object valor = ids.get(0);
        return valor instanceof Number numero ? numero.longValue() : null;
    }
}
