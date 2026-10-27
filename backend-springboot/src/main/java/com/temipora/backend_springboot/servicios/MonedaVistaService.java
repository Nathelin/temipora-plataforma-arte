package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

/**
 * Resuelve la moneda de cobro y las conversiones que se usan para mostrar
 * importes a cada usuario.
 *
 * <p>El cobro siempre se hace en ARS. La moneda preferida del usuario solo
 * cambia cómo se muestra el importe, nunca la moneda en la que se cobra.</p>
 */
@Service
public class MonedaVistaService {

    public static final String CODIGO_COBRO = "ARS";

    private final MonedaService monedaService;
    private final ConversionMonedaService conversionMonedaService;
    private final UsuarioRepository usuarioRepository;

    public MonedaVistaService(
            MonedaService monedaService,
            ConversionMonedaService conversionMonedaService,
            UsuarioRepository usuarioRepository) {
        this.monedaService = monedaService;
        this.conversionMonedaService = conversionMonedaService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Moneda monedaCobro() {
        return monedaService.obtenerPorCodigo(CODIGO_COBRO);
    }

    @Transactional(readOnly = true)
    public Moneda preferidaDe(Long idUsuario) {
        if (idUsuario == null) {
            return null;
        }
        return usuarioRepository.findByIdUsuario(idUsuario)
            .map(Usuario::getMonedaPreferida)
            .orElse(null);
    }

    /**
     * Factor que convierte un importe en {@code origen} a la moneda de cobro.
     */
    @Transactional(readOnly = true)
    public BigDecimal factorCobro(Moneda origen, LocalDateTime fecha) {
        Moneda cobro = monedaCobro();
        if (cobro == null) {
            return null;
        }
        return conversionMonedaService.factor(origen, cobro, fecha);
    }

    /**
     * Factor que convierte un importe en la moneda de cobro a la moneda que
     * prefiere el usuario.
     */
    @Transactional(readOnly = true)
    public BigDecimal factorCobroAMostrada(Moneda mostrada, LocalDateTime fecha) {
        if (mostrada == null) {
            return null;
        }
        Moneda cobro = monedaCobro();
        if (cobro == null) {
            return null;
        }
        return conversionMonedaService.factor(cobro, mostrada, fecha);
    }

    @Transactional(readOnly = true)
    public BigDecimal factorOrigenAMostrada(
            Moneda origen,
            Moneda mostrada,
            LocalDateTime fecha) {
        if (origen == null || mostrada == null) {
            return null;
        }
        return conversionMonedaService.factor(origen, mostrada, fecha);
    }
}
