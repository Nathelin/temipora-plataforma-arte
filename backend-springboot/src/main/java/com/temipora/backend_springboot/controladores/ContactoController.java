package com.temipora.backend_springboot.controladores;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.config.ContactoPlataforma;

@RestController
@RequestMapping("/api/contacto")
public class ContactoController {

    private final ContactoPlataforma contacto;

    public ContactoController(ContactoPlataforma contacto) {
        this.contacto = contacto;
    }

    @GetMapping
    public Map<String, String> obtener() {
        return Map.of("email", contacto.getEmail());
    }
}
