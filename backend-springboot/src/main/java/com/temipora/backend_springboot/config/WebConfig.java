package com.temipora.backend_springboot.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path directorioRaiz;

    public WebConfig(@Value("${storage.local.dir:uploads}") String directorio) {
        this.directorioRaiz = Paths.get(directorio).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(directorioRaiz.toUri().toString())
            // Los archivos se nombran con UUID y nunca se modifican: se pueden
            // cachear de forma indefinida en el navegador y en el túnel.
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365))
                .cachePublic()
                .immutable());
    }
}