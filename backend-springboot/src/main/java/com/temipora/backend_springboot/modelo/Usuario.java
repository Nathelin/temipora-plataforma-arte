package com.temipora.backend_springboot.modelo;


import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="Usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @NotBlank(message = "Por favor, ingrese un nombre de usuario válido.")
    @Size(min = 2, max = 25, message = "El nombre de usuario ingresado debe tener entre 3 y 25 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ0-9]+$")
    private String nombreUsuario;

    @Size(min = 1, max = 25, message = "El nombre de usuario ingresado debe tener entre 3 y 25 caracteres")
    private String nombreDisplay;

    private String biografia;

    @NotBlank(message = "Debe definir una contraseña.")
    private String contrasenia; // puedo ver el tamaño en el front. en el back no pongo size por la encriptación

    @Size(min = 2, max = 25, message = "El nombre ingresado debe tener entre 2 y 25 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre ingresado solo puede contener letras y espacios")
    @Column(nullable = true)
    private String nombre;

    @Size(min = 2, max = 25, message = "El apellido debe tener entre 2 y 25 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido solo puede contener letras y espacios")
    @Column(nullable = true)
    private String apellido;

    /*@Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta", nullable = false, length = 15)
    private EstadoCuenta estadoCuenta = EstadoCuenta.ACTIVA;
    */

    @Column(name = "estado_cuenta", nullable = false, length = 15)
    @Builder.Default
    private boolean estadoCuenta = true;

    @Size(min = 1, max = 15)
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")
    @Column(nullable = true)
    private String sexo;

    @Column(name = "fecha de nacimiento", nullable = true)
    private LocalDate fechaNacimiento;

    @Email
    @Column(nullable = false, length = 40)
    private String correo;

    @Column(nullable = true, length = 20)
    private String telefono;

    @Column(name = "fechaCreacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "UltimaConexion")
    private LocalDateTime fechaUltimaConexion;

    @Column(name = "pfp", nullable = true)
    private String urlFotoPerfil;

    @Column(name = "pfp_thumbnail", nullable = true)
    private String urlFotoPerfilMiniatura;
    
    @Column(name = "banner", nullable = true)
    private String urlBanner;

    // TODO: Ver si manejo estadoCuenta con booleano o enum........

    public void activar() {
        this.estadoCuenta = true;
    }

    public void desactivar() {
        this.estadoCuenta = false;
    }

    /*@ManyToOne(targetEntity = Rol.class, cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(name="usuarios-rol", joinColumns= @JoinColumn(name="idUsuario"), inverseJoinColumns = @JoinColumn(name="idRol"))
    private Set<Rol> roles;*/
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idRol", nullable = false)
    private Rol rol;

}