package com.temipora.backend_springboot.config;

import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String key; // firma de que yo genero el token
    
    @Value("${jwt.time.expiration}")
    private String vencimientoSesion; // TODO: ver cuánto tiempo de sesión permito...

    public String generarTokenAcceso(String nombreUsuario){
        return Jwts.builder()
            .subject(nombreUsuario)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + Long.parseLong(vencimientoSesion)))
            .signWith(obtenerFirma())
            .compact();
    }

    public boolean tokenEsValido(String token){
        try {
            Jwts.parser()                  
                .verifyWith(obtenerFirma()) 
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
           return false;
        }

    }

    public SecretKey obtenerFirma(){
        byte[] bytesKey = Decoders.BASE64.decode(key);
        return Keys.hmacShaKeyFor(bytesKey);
    }

    public String obtenerNombreUsuarioToken(String token) {
        return obtenerUnClaim(token, claims -> claims.getSubject());
    }


    public <T> T obtenerUnClaim(String token, Function <Claims,T> claimTFunction){
        Claims claim = obtenerClaimsToken(token);
        return claimTFunction.apply(claim);
    }

    public Claims obtenerClaimsToken(String token){ // las partes decodificadas
        return Jwts.parser()                  
                .verifyWith(obtenerFirma()) 
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

