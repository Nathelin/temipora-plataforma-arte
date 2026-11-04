package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.AlertaAdminDTO;
import com.temipora.backend_springboot.repositorios.EventoAuditoriaRepository;

/**
 * Reglas de seguridad y fraude calculadas sobre la auditoría. Los umbrales
 * viven acá a propósito: modificarlos desde el panel requiere validación y
 * auditoría propios, y un admin comprometido podría usarlos para tapar
 * actividad. Cuando haya datos reales se pueden mover a configuración.
 */
@Service
public class AlertasAdminService {

    private static final int LOGIN_USUARIO_UMBRAL = 5;
    private static final int LOGIN_USUARIO_MINUTOS = 15;
    private static final int LOGIN_GLOBAL_UMBRAL = 20;
    private static final int LOGIN_GLOBAL_MINUTOS = 60;

    private static final int SEGURIDAD_UMBRAL = 2;
    private static final int SEGURIDAD_MINUTOS = 60;
    private static final List<String> SEGURIDAD_ACCIONES = List.of(
        AccionesAdmin.USUARIO_CAMBIAR_CONTRASENIA,
        AccionesAdmin.USUARIO_CAMBIAR_CORREO,
        AccionesAdmin.USUARIO_CAMBIAR_TELEFONO,
        AccionesAdmin.USUARIO_QUITAR_TELEFONO);

    private static final int PAGOS_RECHAZADOS_UMBRAL = 3;
    private static final int PEDIDOS_CANCELADOS_UMBRAL = 5;
    private static final int HITOS_VENCIDOS_UMBRAL = 3;
    private static final int VENTANA_HORAS = 24;

    private static final int CATALOGO_UMBRAL = 10;
    private static final int CATALOGO_MINUTOS = 60;
    private static final List<String> CATALOGO_ACCIONES = List.of(
        AccionesAdmin.SERVICIO_MODIFICAR,
        AccionesAdmin.SERVICIO_ARCHIVAR,
        AccionesAdmin.SERVICIO_PAUSAR);

    private static final int MAX_POR_GRUPO = 5;

    private final EventoAuditoriaRepository eventoRepository;

    public AlertasAdminService(EventoAuditoriaRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertaAdminDTO> listar() {
        LocalDateTime ahora = LocalDateTime.now();
        List<AlertaAdminDTO> alertas = new ArrayList<>();

        alertasLoginsPorUsuario(alertas, ahora);
        alertasLoginGlobal(alertas, ahora);
        alertasSeguridad(alertas, ahora);
        alertasPagos(alertas, ahora);
        alertasPedidos(alertas, ahora);
        alertasHitos(alertas, ahora);
        alertasCatalogo(alertas, ahora);

        alertas.sort(Comparator
            .comparingInt((AlertaAdminDTO alerta) -> peso(alerta.severidad()))
            .reversed()
            .thenComparing(
                Comparator.comparingLong(AlertaAdminDTO::cantidad).reversed()));
        return alertas;
    }

    private void alertasLoginsPorUsuario(
            List<AlertaAdminDTO> alertas, LocalDateTime ahora) {
        LocalDateTime desde = ahora.minusMinutes(LOGIN_USUARIO_MINUTOS);
        eventoRepository
            .contarPorActor(AccionesAdmin.LOGIN_FALLIDO, desde, LOGIN_USUARIO_UMBRAL)
            .stream()
            .limit(MAX_POR_GRUPO)
            .forEach(fila -> {
                String actor = (String) fila[0];
                long cantidad = numero(fila[1]);
                alertas.add(new AlertaAdminDTO(
                    "LOGIN_FALLIDO_USUARIO",
                    "ALTA",
                    "Posible fuerza bruta",
                    "El usuario @" + actor + " acumuló " + cantidad
                        + " inicios de sesión fallidos en los últimos "
                        + LOGIN_USUARIO_MINUTOS + " minutos.",
                    cantidad,
                    desde,
                    ahora,
                    AccionesAdmin.LOGIN_FALLIDO,
                    actor));
            });
    }

    private void alertasLoginGlobal(
            List<AlertaAdminDTO> alertas, LocalDateTime ahora) {
        LocalDateTime desde = ahora.minusMinutes(LOGIN_GLOBAL_MINUTOS);
        long cantidad = eventoRepository.countByAccionAndOcurridoEnBetween(
            AccionesAdmin.LOGIN_FALLIDO, desde, ahora);
        if (cantidad >= LOGIN_GLOBAL_UMBRAL) {
            alertas.add(new AlertaAdminDTO(
                "LOGIN_FALLIDO_GLOBAL",
                "MEDIA",
                "Aumento de inicios de sesión fallidos",
                "Se registraron " + cantidad
                    + " intentos fallidos en la última hora.",
                cantidad,
                desde,
                ahora,
                AccionesAdmin.LOGIN_FALLIDO,
                null));
        }
    }

    private void alertasSeguridad(
            List<AlertaAdminDTO> alertas, LocalDateTime ahora) {
        LocalDateTime desde = ahora.minusMinutes(SEGURIDAD_MINUTOS);
        eventoRepository
            .contarPorActorEnAcciones(
                SEGURIDAD_ACCIONES, desde, SEGURIDAD_UMBRAL)
            .stream()
            .limit(MAX_POR_GRUPO)
            .forEach(fila -> {
                String actor = (String) fila[0];
                long cantidad = numero(fila[1]);
                alertas.add(new AlertaAdminDTO(
                    "SEGURIDAD_CAMBIOS",
                    "ALTA",
                    "Cambios de seguridad seguidos",
                    "El usuario @" + actor + " hizo " + cantidad
                        + " cambios de seguridad en la última hora.",
                    cantidad,
                    desde,
                    ahora,
                    null,
                    actor));
            });
    }

    private void alertasPagos(
            List<AlertaAdminDTO> alertas, LocalDateTime ahora) {
        LocalDateTime desde = ahora.minusHours(VENTANA_HORAS);
        long cantidad = eventoRepository.countByAccionAndOcurridoEnBetween(
            AccionesAdmin.PAGO_RECHAZAR, desde, ahora);
        if (cantidad >= PAGOS_RECHAZADOS_UMBRAL) {
            alertas.add(new AlertaAdminDTO(
                "PAGOS_RECHAZADOS",
                "MEDIA",
                "Pagos rechazados",
                "Se rechazaron " + cantidad + " pagos en las últimas "
                    + VENTANA_HORAS + " horas.",
                cantidad,
                desde,
                ahora,
                AccionesAdmin.PAGO_RECHAZAR,
                null));
        }
    }

    private void alertasPedidos(
            List<AlertaAdminDTO> alertas, LocalDateTime ahora) {
        LocalDateTime desde = ahora.minusHours(VENTANA_HORAS);
        long cantidad = eventoRepository.countByAccionAndOcurridoEnBetween(
            AccionesAdmin.PEDIDO_CANCELAR, desde, ahora);
        if (cantidad >= PEDIDOS_CANCELADOS_UMBRAL) {
            alertas.add(new AlertaAdminDTO(
                "PEDIDOS_CANCELADOS",
                "MEDIA",
                "Cancelaciones de pedidos",
                "Se cancelaron " + cantidad + " pedidos en las últimas "
                    + VENTANA_HORAS + " horas.",
                cantidad,
                desde,
                ahora,
                AccionesAdmin.PEDIDO_CANCELAR,
                null));
        }
    }

    private void alertasHitos(
            List<AlertaAdminDTO> alertas, LocalDateTime ahora) {
        LocalDateTime desde = ahora.minusHours(VENTANA_HORAS);
        long cantidad = eventoRepository.countByAccionAndOcurridoEnBetween(
            AccionesAdmin.HITO_VENCER, desde, ahora);
        if (cantidad >= HITOS_VENCIDOS_UMBRAL) {
            alertas.add(new AlertaAdminDTO(
                "HITOS_VENCIDOS",
                "MEDIA",
                "Hitos vencidos",
                "Venció el pago de " + cantidad + " hitos en las últimas "
                    + VENTANA_HORAS + " horas.",
                cantidad,
                desde,
                ahora,
                AccionesAdmin.HITO_VENCER,
                null));
        }
    }

    private void alertasCatalogo(
            List<AlertaAdminDTO> alertas, LocalDateTime ahora) {
        LocalDateTime desde = ahora.minusMinutes(CATALOGO_MINUTOS);
        eventoRepository
            .contarPorActorEnAcciones(
                CATALOGO_ACCIONES, desde, CATALOGO_UMBRAL)
            .stream()
            .limit(MAX_POR_GRUPO)
            .forEach(fila -> {
                String actor = (String) fila[0];
                long cantidad = numero(fila[1]);
                alertas.add(new AlertaAdminDTO(
                    "CATALOGO_EDICION_MASIVA",
                    "MEDIA",
                    "Edición masiva de servicios",
                    "El actor @" + actor + " modificó " + cantidad
                        + " servicios en la última hora.",
                    cantidad,
                    desde,
                    ahora,
                    null,
                    actor));
            });
    }

    private long numero(Object valor) {
        return valor instanceof Number numero ? numero.longValue() : 0L;
    }

    private int peso(String severidad) {
        if ("ALTA".equals(severidad)) {
            return 3;
        }
        if ("MEDIA".equals(severidad)) {
            return 2;
        }
        return 1;
    }
}
