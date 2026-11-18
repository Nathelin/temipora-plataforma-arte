package com.temipora.backend_springboot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ContactoPlataforma {

    private final String email;

    public ContactoPlataforma(
            @Value("${temipora.contacto.email:temiporasupport@gmail.com}") String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
