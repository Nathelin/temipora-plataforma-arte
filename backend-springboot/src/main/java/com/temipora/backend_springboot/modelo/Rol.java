package com.temipora.backend_springboot.modelo;


import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="Rol")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRol;

    @Enumerated(EnumType.STRING)
    private ERol nombre;
    // ver si lo dejo como enum. en principio si

    @NotNull
    @Builder.Default
    private boolean estado = true;

    /**
     * Rol del que este rol hereda funciones. La jerarquía es lineal:
     * CLIENTE <- ARTISTA <- ADMIN. Un rol sólo hereda de su padre.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol_padre")
    private Rol rolPadre;

    /*@OneToMany(mappedBy = "rol", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    //@JoinTable(name="usuarios-rol", joinColumns= @JoinColumn(name="idUsuario"), inverseJoinColumns = @JoinColumn(name="idRol"))
    private Set<Usuario> usuarios;*/
    @OneToMany(mappedBy = "rol", fetch = FetchType.LAZY)
    private Set<Usuario> usuarios;

}
