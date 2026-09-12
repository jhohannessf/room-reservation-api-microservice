package br.com.jhohannesfreitas.room_ms.infra.config;

// Classe responsável por gerar e validar o token

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
public class TokenProvider {

    @Value("${jwt.expiration}")
    private long expirationTime;

    @Value("${jwt.key}")
    private String key;

    // Gerar o token
    public String gerarToken(Authentication authentication) {
        UserDetails user = (UserDetails) authentication.getPrincipal();
        return buildToken(user.getUsername(), authentication);
    }

    private String buildToken(String username, Authentication authentication) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTime);

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .claim("authorities", authorities)
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(key.getBytes());
    }

    // Validar o token
    public boolean isTokenValid(String token) {
        try{
            getClaimsFromToken(token);
            return true;
        } catch (Exception e){
            return false;
        }
    }

    // Extrair informações do token
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    private Claims getClaimsFromToken(String token) {
        //validar assinatura
        // validar expiração
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public LocalDateTime getExpirationDateToken() {
        // Data de expiração
        Date now = new Date(); // Pega a data e hora atual
        Date expirationDate = new Date(now.getTime() + expirationTime);

        return expirationDate.toInstant()
                .atZone(ZoneId.of("America/Sao_Paulo"))
                .toLocalDateTime();
    }

    public List<String> getAuthoritiesFromToken(String token) {
        return getClaimsFromToken(token).get("authorities", List.class);
    }
}
