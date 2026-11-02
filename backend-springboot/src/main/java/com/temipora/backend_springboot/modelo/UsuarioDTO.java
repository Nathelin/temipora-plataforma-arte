package com.temipora.backend_springboot.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.temipora.backend_springboot.servicios.TelefonoService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDTO {

    private Long idUsuario;
    private String nombreUsuario;
    private String nombreDisplay;
    private String nombre;
    private String apellido;
    private String biografia;
    private String correo;
    private boolean tieneTelefono;
    private String telefonoEnmascarado;
    private String sexo;
    private Long idPais;
    private String nombrePais;
    private LocalDate fechaNacimiento;
    private boolean estadoCuenta;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimaConexion;
    private String urlFotoPerfil;
    private String urlFotoPerfilMiniatura;
    private String urlBanner;
    private EPreferenciaContenidoSensible preferenciaSensible;
    private boolean pedidosPublicos;
    private boolean recibirEmailsPromocionales;
    private Long idMonedaPreferida;
    private String monedaPreferida;
    private String rol;

    public static UsuarioDTO desdeUsuario(Usuario usuario) {
        return UsuarioDTO.builder()
            .idUsuario(usuario.getIdUsuario())
            .nombreUsuario(usuario.getNombreUsuario())
            .nombreDisplay(usuario.getNombreDisplay() != null && !usuario.getNombreDisplay().isBlank()
                ? usuario.getNombreDisplay()
                : usuario.getNombreUsuario())
            .nombre(usuario.getNombre())
            .apellido(usuario.getApellido())
            .biografia(usuario.getBiografia())
            .correo(usuario.getCorreo())
            .tieneTelefono(usuario.getTelefono() != null && !usuario.getTelefono().isBlank())
            .telefonoEnmascarado(TelefonoService.enmascarar(usuario.getTelefono()))
            .sexo(usuario.getSexo())
            .idPais(usuario.getPais() != null ? usuario.getPais().getIdPais() : null)
            .nombrePais(usuario.getPais() != null ? usuario.getPais().getNombre() : null)
            .fechaNacimiento(usuario.getFechaNacimiento())
            .estadoCuenta(usuario.isEstadoCuenta())
            .fechaCreacion(usuario.getFechaCreacion())
            .fechaUltimaConexion(usuario.getFechaUltimaConexion())
            .urlFotoPerfil(usuario.getUrlFotoPerfil())
            .urlFotoPerfilMiniatura(usuario.getUrlFotoPerfilMiniatura())
            .urlBanner(usuario.getUrlBanner())
            .preferenciaSensible(usuario.getPreferenciaSensible())
            .pedidosPublicos(usuario.isPedidosPublicos())
            .recibirEmailsPromocionales(usuario.isRecibirEmailsPromocionales())
            .idMonedaPreferida(usuario.getMonedaPreferida() != null
                ? usuario.getMonedaPreferida().getIdMoneda() : null)
            .monedaPreferida(usuario.getMonedaPreferida() != null
                ? usuario.getMonedaPreferida().getCodigo() : null)
            .rol(usuario.getRol() != null ? usuario.getRol().getNombre().name() : null)
            .build();
    }
}