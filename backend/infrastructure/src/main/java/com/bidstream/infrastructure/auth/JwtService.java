package com.bidstream.infrastructure.auth;

import com.bidstream.application.auth.port.JwtPort;
import com.bidstream.domain.user.Role;
import com.bidstream.domain.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService implements JwtPort {

  private final SecretKey secretKey;
  private final long accessTokenTtlSeconds;

  public JwtService(
      @Value("${bidstream.auth.jwt-secret}") String jwtSecret,
      @Value("${bidstream.auth.access-token-ttl-seconds:900}") long accessTokenTtlSeconds) {
    this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenTtlSeconds = accessTokenTtlSeconds;
  }

  @Override
  public String createAccessToken(User user) {
    Instant now = Instant.now();
    List<String> roles = user.roles().stream().map(Role::name).sorted().toList();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(String.valueOf(user.id()))
        .claim("email", user.email())
        .claim("roles", roles)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
        .signWith(secretKey)
        .compact();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Optional<AccessTokenClaims> parseAccessToken(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
      long userId = Long.parseLong(claims.getSubject());
      String email = claims.get("email", String.class);
      List<String> roles = claims.get("roles", List.class);
      String jti = claims.getId();
      return Optional.of(new AccessTokenClaims(userId, email, roles, jti));
    } catch (JwtException | NumberFormatException ex) {
      return Optional.empty();
    }
  }
}
