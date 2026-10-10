package com.temipora.backend_springboot.servicios;

import org.springframework.stereotype.Service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

@Service
public class TelefonoService {

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

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
}
