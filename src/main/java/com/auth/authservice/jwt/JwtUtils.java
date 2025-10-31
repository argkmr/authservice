package com.auth.authservice.jwt;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationInMs;

    public String generateToken(UserDetails userDetails) {
        Key signingKey = getSigningKey();
        Date expirationDate = getExpiratonDate();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("roles", roles) // plural
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Date getExpiratonDate(){
        return new Date(System.currentTimeMillis() + expirationInMs);
    }

    public String extractUsername(String token){
        Key signingKey = getSigningKey();
        Claims claims =  Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey));
    }
}
