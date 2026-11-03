package com.temipora.backend_springboot.servicios;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clasificación única de las acciones de auditoría. Se usa para filtrar por
 * categoría, marcar eventos críticos y alimentar el selector del panel.
 */
public final class CatalogoAuditoria {

    private static final Map<String, CategoriaAuditoria> CATEGORIAS =
        new HashMap<>();
    private static final Set<String> CRITICAS = new LinkedHashSet<>();

    static {
        registrar(CategoriaAuditoria.COMERCIO,
            AccionesAdmin.PRESUPUESTO_CREAR,
            AccionesAdmin.PEDIDO_CREAR,
            AccionesAdmin.PEDIDO_CANCELAR,
            AccionesAdmin.PEDIDO_FINALIZAR,
            AccionesAdmin.PEDIDO_PUBLICACION_AUTORIZAR,
            AccionesAdmin.PEDIDO_PUBLICACION_RETIRAR,
            AccionesAdmin.PEDIDO_GALERIA_OCULTAR,
            AccionesAdmin.PEDIDO_GALERIA_MOSTRAR,
            AccionesAdmin.PEDIDO_PROCESAR,
            AccionesAdmin.HITO_APROBAR,
            AccionesAdmin.HITO_REVISION,
            AccionesAdmin.HITO_APROBAR_AUTOMATICO,
            AccionesAdmin.HITO_VENCER,
            AccionesAdmin.ENTREGA_CREAR,
            AccionesAdmin.PAGO_INICIAR,
            AccionesAdmin.PAGO_CONFIRMAR,
            AccionesAdmin.PAGO_ESTADO_ACTUALIZAR,
            AccionesAdmin.PAGO_RECHAZAR,
            AccionesAdmin.SOLICITUD_CREAR,
            AccionesAdmin.SOLICITUD_RECHAZAR,
            AccionesAdmin.SOLICITUD_CANCELAR,
            AccionesAdmin.SOLICITUD_RETIRAR_PRESUPUESTO);

        registrar(CategoriaAuditoria.USUARIOS,
            AccionesAdmin.USUARIO_CAMBIAR_CORREO,
            AccionesAdmin.USUARIO_CAMBIAR_CONTRASENIA,
            AccionesAdmin.USUARIO_CAMBIAR_TELEFONO,
            AccionesAdmin.USUARIO_QUITAR_TELEFONO,
            AccionesAdmin.LOGIN_FALLIDO);

        registrar(CategoriaAuditoria.MODERACION,
            AccionesAdmin.RESENIA_CREAR,
            AccionesAdmin.RESENIA_EDITAR,
            AccionesAdmin.RESENIA_ELIMINAR,
            AccionesAdmin.RESENIA_REPORTAR,
            AccionesAdmin.RESENIA_OCULTAR,
            AccionesAdmin.RESENIA_RESTAURAR,
            AccionesAdmin.RESENIA_DESCARTAR_REPORTES);

        registrar(CategoriaAuditoria.CATALOGO,
            AccionesAdmin.ETIQUETA_CREAR,
            AccionesAdmin.ETIQUETA_MODIFICAR,
            AccionesAdmin.ETIQUETA_APROBAR,
            AccionesAdmin.ETIQUETA_RECHAZAR,
            AccionesAdmin.ETIQUETA_BAJA,
            AccionesAdmin.ETIQUETA_REACTIVAR,
            AccionesAdmin.CATEGORIA_CREAR,
            AccionesAdmin.CATEGORIA_MODIFICAR,
            AccionesAdmin.CATEGORIA_BAJA,
            AccionesAdmin.CATEGORIA_REACTIVAR,
            AccionesAdmin.MEDIO_CREAR,
            AccionesAdmin.MEDIO_MODIFICAR,
            AccionesAdmin.MEDIO_BAJA,
            AccionesAdmin.MEDIO_REACTIVAR,
            AccionesAdmin.MATERIAL_CREAR,
            AccionesAdmin.MATERIAL_MODIFICAR,
            AccionesAdmin.MATERIAL_BAJA,
            AccionesAdmin.MATERIAL_REACTIVAR,
            AccionesAdmin.TIPO_ARTE_CREAR,
            AccionesAdmin.TIPO_ARTE_MODIFICAR,
            AccionesAdmin.TIPO_ARTE_BAJA,
            AccionesAdmin.TIPO_ARTE_REACTIVAR,
            AccionesAdmin.TIPO_PRODUCTO_CREAR,
            AccionesAdmin.TIPO_PRODUCTO_MODIFICAR,
            AccionesAdmin.TIPO_PRODUCTO_BAJA,
            AccionesAdmin.TIPO_PRODUCTO_REACTIVAR,
            AccionesAdmin.TIPO_SERVICIO_CREAR,
            AccionesAdmin.TIPO_SERVICIO_MODIFICAR,
            AccionesAdmin.TIPO_SERVICIO_BAJA,
            AccionesAdmin.TIPO_SERVICIO_REACTIVAR,
            AccionesAdmin.PAIS_CREAR,
            AccionesAdmin.PAIS_MODIFICAR,
            AccionesAdmin.PAIS_BAJA,
            AccionesAdmin.PAIS_REACTIVAR,
            AccionesAdmin.MONEDA_CREAR,
            AccionesAdmin.MONEDA_MODIFICAR,
            AccionesAdmin.MONEDA_BAJA,
            AccionesAdmin.MONEDA_REACTIVAR,
            AccionesAdmin.MONEDA_PREDETERMINADA,
            AccionesAdmin.SERVICIO_CREAR,
            AccionesAdmin.SERVICIO_MODIFICAR,
            AccionesAdmin.SERVICIO_PAUSAR,
            AccionesAdmin.SERVICIO_REACTIVAR,
            AccionesAdmin.SERVICIO_ARCHIVAR,
            AccionesAdmin.SERVICIO_RESTAURAR);

        registrar(CategoriaAuditoria.CONFIGURACION,
            AccionesAdmin.COMISION_ACTUALIZAR,
            AccionesAdmin.PRESUPUESTO_CONFIG_ACTUALIZAR,
            AccionesAdmin.PEDIDO_CONFIG_ACTUALIZAR,
            AccionesAdmin.MENSAJERIA_CONFIG_ACTUALIZAR,
            AccionesAdmin.RESENIA_CONFIG_ACTUALIZAR,
            AccionesAdmin.INACTIVIDAD_CONFIG_ACTUALIZAR,
            AccionesAdmin.REACTIVACION_EJECUTAR,
            AccionesAdmin.COTIZACION_ACTUALIZAR,
            AccionesAdmin.CRITERIO_CREAR,
            AccionesAdmin.CRITERIO_MODIFICAR,
            AccionesAdmin.CRITERIO_BAJA,
            AccionesAdmin.CRITERIO_REACTIVAR,
            AccionesAdmin.CRITERIO_OPCION_CREAR,
            AccionesAdmin.CRITERIO_OPCION_MODIFICAR,
            AccionesAdmin.CRITERIO_OPCION_BAJA,
            AccionesAdmin.CRITERIO_OPCION_REACTIVAR);

        registrar(CategoriaAuditoria.LEGAL,
            AccionesAdmin.TERMINOS_PUBLICAR,
            AccionesAdmin.TERMINOS_ACEPTAR);

        registrar(CategoriaAuditoria.SISTEMA,
            AccionesAdmin.COTIZACION_REFRESCAR);

        criticas(
            AccionesAdmin.PRESUPUESTO_CREAR,
            AccionesAdmin.PEDIDO_CREAR,
            AccionesAdmin.PEDIDO_CANCELAR,
            AccionesAdmin.PEDIDO_FINALIZAR,
            AccionesAdmin.PEDIDO_PUBLICACION_AUTORIZAR,
            AccionesAdmin.PEDIDO_PUBLICACION_RETIRAR,
            AccionesAdmin.PEDIDO_GALERIA_OCULTAR,
            AccionesAdmin.PEDIDO_GALERIA_MOSTRAR,
            AccionesAdmin.PEDIDO_PROCESAR,
            AccionesAdmin.HITO_APROBAR,
            AccionesAdmin.HITO_REVISION,
            AccionesAdmin.HITO_APROBAR_AUTOMATICO,
            AccionesAdmin.HITO_VENCER,
            AccionesAdmin.ENTREGA_CREAR,
            AccionesAdmin.PAGO_INICIAR,
            AccionesAdmin.PAGO_CONFIRMAR,
            AccionesAdmin.PAGO_ESTADO_ACTUALIZAR,
            AccionesAdmin.PAGO_RECHAZAR,
            AccionesAdmin.SOLICITUD_CREAR,
            AccionesAdmin.SOLICITUD_RECHAZAR,
            AccionesAdmin.SOLICITUD_CANCELAR,
            AccionesAdmin.SOLICITUD_RETIRAR_PRESUPUESTO,
            AccionesAdmin.TERMINOS_PUBLICAR,
            AccionesAdmin.TERMINOS_ACEPTAR,
            AccionesAdmin.USUARIO_CAMBIAR_CORREO,
            AccionesAdmin.USUARIO_CAMBIAR_CONTRASENIA,
            AccionesAdmin.USUARIO_CAMBIAR_TELEFONO,
            AccionesAdmin.USUARIO_QUITAR_TELEFONO,
            AccionesAdmin.RESENIA_OCULTAR,
            AccionesAdmin.RESENIA_RESTAURAR,
            AccionesAdmin.RESENIA_DESCARTAR_REPORTES);
    }

    private CatalogoAuditoria() {
    }

    public static CategoriaAuditoria categoriaDe(String accion) {
        if (accion == null) {
            return CategoriaAuditoria.OTROS;
        }
        return CATEGORIAS.getOrDefault(accion, CategoriaAuditoria.OTROS);
    }

    public static boolean esCritica(String accion) {
        return accion != null && CRITICAS.contains(accion);
    }

    public static Set<String> accionesDe(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return Set.of();
        }
        CategoriaAuditoria buscada;
        try {
            buscada = CategoriaAuditoria.valueOf(
                categoria.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Set.of();
        }
        Set<String> acciones = new LinkedHashSet<>();
        CATEGORIAS.forEach((accion, cat) -> {
            if (cat == buscada) {
                acciones.add(accion);
            }
        });
        return Collections.unmodifiableSet(acciones);
    }

    public static Set<String> accionesCriticas() {
        return Collections.unmodifiableSet(CRITICAS);
    }

    public static List<CategoriaAuditoria> categorias() {
        return List.of(CategoriaAuditoria.values());
    }

    private static void registrar(
            CategoriaAuditoria categoria, String... acciones) {
        for (String accion : acciones) {
            CATEGORIAS.put(accion, categoria);
        }
    }

    private static void criticas(String... acciones) {
        CRITICAS.addAll(List.of(acciones));
    }
}
