package com.temipora.backend_springboot.servicios;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Foto de los campos auditables de una entidad, tomada antes de modificarla.
 * Se leen solo las propiedades indicadas, no toda la fila.
 */
public final class Snapshot {

    private final Map<String, Object> valores;

    private Snapshot(Map<String, Object> valores) {
        this.valores = valores;
    }

    public static Snapshot de(Object entidad, List<CampoAuditado> campos) {
        Map<String, Object> valores = new LinkedHashMap<>();
        for (CampoAuditado campo : campos) {
            valores.put(campo.campo(), valorLegible(valorDe(entidad, campo.campo())));
        }
        return new Snapshot(valores);
    }

    public Object get(String campo) {
        return valores.get(campo);
    }

    public static Object valorDe(Object entidad, String campo) {
        if (entidad == null) {
            return null;
        }
        String sufijo = Character.toUpperCase(campo.charAt(0)) + campo.substring(1);
        try {
            Method getter = entidad.getClass().getMethod("get" + sufijo);
            return getter.invoke(entidad);
        } catch (ReflectiveOperationException e) {
            try {
                Method getter = entidad.getClass().getMethod("is" + sufijo);
                return getter.invoke(entidad);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
    }

    public static Object valorLegible(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof String || valor instanceof Number || valor instanceof Boolean) {
            return valor;
        }
        if (valor instanceof Enum<?> enumerado) {
            return enumerado.name();
        }
        Object nombre = valorDe(valor, "nombre");
        if (nombre != null) {
            return nombre;
        }
        Object codigo = valorDe(valor, "codigo");
        if (codigo != null) {
            return codigo;
        }
        return null;
    }
}
