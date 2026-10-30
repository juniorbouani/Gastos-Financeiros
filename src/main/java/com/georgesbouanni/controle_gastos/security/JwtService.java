package com.georgesbouanni.controle_gastos.security;

import com.georgesbouanni.controle_gastos.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {
    private final UserRepository users;

    public JwtService(UserRepository users) { this.users = users; }


    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateToken(String cpf) {
        var user = users.findByCpf(cpf).filter(u -> !u.isAccountClosed())
                .orElseThrow(() -> new IllegalArgumentException("Conta indisponível."));
        return Jwts.builder()
                .subject(cpf)
                .claim("cv", user.getCredentialVersion())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractCpf(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, String cpf) {
        Claims claims = extractAllClaims(token);
        Number version = claims.get("cv", Number.class);
        int tokenVersion = version == null ? 0 : version.intValue();
        return cpf.equals(claims.getSubject()) && claims.getExpiration().after(new Date())
                && users.findByCpf(cpf).filter(u -> !u.isAccountClosed())
                .map(u -> u.getCredentialVersion() == tokenVersion).orElse(false);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}