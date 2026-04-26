package br.com.fiap.oficina.seguranca.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${oficina.jwt.secret}")
    private String secret;

    @Value("${oficina.jwt.expiracao}")
    private long expiracao;

    public String gerarToken(String login) {
        return Jwts.builder()
                .subject(login)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiracao))
                .signWith(getChave())
                .compact();
    }

    public String extrairLogin(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public boolean validarToken(String token, UserDetails userDetails) {
        try {
            String login = extrairLogin(token);
            return login.equals(userDetails.getUsername()) && !isTokenExpirado(token);
        } catch (JwtException e) {
            return false;
        }
    }

    private boolean isTokenExpirado(String token) {
        return extrairClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extrairClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getChave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    private SecretKey getChave() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
