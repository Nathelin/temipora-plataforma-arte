package com.temipora.backend_springboot.servicios;

import org.springframework.stereotype.Service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

@Service
public class TelefonoService {

    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();
    private static final int DIGITOS_VISIBLES = 4;

    private final PhoneNumberUtil phoneNumberUtil = PHONE_NUMBER_UTIL;

    /**
     * Valida el número contra el país indicado (código ISO alpha-2) y lo normaliza a E.164.
     * Si el número viene con prefijo internacional (+...), la región es opcional.
     */
    public String validarYNormalizar(String numero, String codigoIso) {
        String limpio = numero == null ? "" : numero.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El teléfono no puede estar vacío.");
        }

        String region = (codigoIso != null && !codigoIso.isBlank())
                ? codigoIso.trim().toUpperCase()
                : null;

        try {
            PhoneNumber parsed = phoneNumberUtil.parse(limpio, region);
            if (!phoneNumberUtil.isValidNumber(parsed)) {
                throw new IllegalArgumentException("El teléfono no es válido para el país seleccionado.");
            }
            return phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException(
                    "El teléfono no es válido. Revisá el número y el país seleccionado.");
        }
    }

    /**
     * Devuelve una versión enmascarada del número, conservando el prefijo
     * internacional y los últimos 4 dígitos.
     */
    public static String enmascarar(String numero) {
        if (numero == null || numero.isBlank()) {
            return null;
        }

        String limpio = numero.trim();
        try {
            PhoneNumber parsed = PHONE_NUMBER_UTIL.parse(limpio, null);
            String nacional = String.valueOf(parsed.getNationalNumber());
            return "+" + parsed.getCountryCode() + " " + enmascararDigitos(nacional);
        } catch (NumberParseException e) {
            String soloDigitos = limpio.replaceAll("\\D", "");
            if (soloDigitos.isEmpty()) {
                return null;
            }
            return enmascararDigitos(soloDigitos);
        }
    }

    private static String enmascararDigitos(String digitos) {
        if (digitos.length() <= DIGITOS_VISIBLES) {
            return "*".repeat(digitos.length());
        }
        int ocultos = digitos.length() - DIGITOS_VISIBLES;
        return "*".repeat(ocultos) + digitos.substring(ocultos);
    }
}
