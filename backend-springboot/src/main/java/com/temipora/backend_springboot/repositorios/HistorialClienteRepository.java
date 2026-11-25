package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class HistorialClienteRepository {

    private static final String FILTRO = """
        where h.id_cliente = :idCliente
          and h.fecha >= :desde
          and h.fecha < :hasta
          and (:tipo = '' or h.tipo = :tipo)
          and (:estadoGrupo = ''
               or (:estadoGrupo = 'ABIERTO'
                   and h.estado_grupo in ('EN_CURSO', 'PENDIENTE'))
               or h.estado_grupo = :estadoGrupo)
          and (:patron = ''
               or lower(h.titulo) like '%' || :patron || '%'
               or lower(h.contraparte) like '%' || :patron || '%')
        """;

    private static final String SELECCION = """
        select h.tipo, h.id_referencia, h.fecha, h.titulo, h.contraparte,
               h.monto, h.moneda, h.estado, h.estado_grupo,
               h.descuento_porcentaje, h.descuento_monto,
               h.cantidad_items, h.cantidad_grupos
        from v_historial_cliente h
        """;

    private static final String ORDEN =
        " order by h.fecha desc, h.id_referencia desc";

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> buscar(
            Long idCliente,
            LocalDateTime desde,
            LocalDateTime hasta,
            String tipo,
            String estadoGrupo,
            String patron,
            int pagina,
            int tamanio) {
        Query query = entityManager.createNativeQuery(
            SELECCION + FILTRO + ORDEN);
        aplicarParametros(
            query, idCliente, desde, hasta, tipo, estadoGrupo, patron);
        query.setFirstResult(pagina * tamanio);
        query.setMaxResults(tamanio);
        return query.getResultList();
    }

    public long contar(
            Long idCliente,
            LocalDateTime desde,
            LocalDateTime hasta,
            String tipo,
            String estadoGrupo,
            String patron) {
        Query query = entityManager.createNativeQuery(
            "select count(*) from v_historial_cliente h " + FILTRO);
        aplicarParametros(
            query, idCliente, desde, hasta, tipo, estadoGrupo, patron);
        return ((Number) query.getSingleResult()).longValue();
    }

    public List<Object[]> resumir(
            Long idCliente,
            LocalDateTime desde,
            LocalDateTime hasta,
            String tipo,
            String estadoGrupo,
            String patron) {
        Query query = entityManager.createNativeQuery("""
            select h.tipo, h.estado_grupo, h.moneda, count(*),
                   coalesce(sum(h.monto), 0),
                   coalesce(sum(h.descuento_monto), 0)
            from v_historial_cliente h
            """ + FILTRO + """
            group by h.tipo, h.estado_grupo, h.moneda
            """);
        aplicarParametros(
            query, idCliente, desde, hasta, tipo, estadoGrupo, patron);
        return query.getResultList();
    }

    private void aplicarParametros(
            Query query,
            Long idCliente,
            LocalDateTime desde,
            LocalDateTime hasta,
            String tipo,
            String estadoGrupo,
            String patron) {
        query.setParameter("idCliente", idCliente);
        query.setParameter("desde", desde);
        query.setParameter("hasta", hasta);
        query.setParameter("tipo", tipo);
        query.setParameter("estadoGrupo", estadoGrupo);
        query.setParameter("patron", patron);
    }
}
