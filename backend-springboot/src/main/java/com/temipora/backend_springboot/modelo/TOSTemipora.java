package com.temipora.backend_springboot.modelo;


import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="AcceptacionTOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TOSTemipora { 

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAceptacion;

    private String versionTOS;

    private LocalDateTime fechaAceptacion;

    // 1 usuario tiene N aceptaciones únicas de los TOS de la página

}
