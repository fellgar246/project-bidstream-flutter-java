package com.bidstream.api.security;

import com.bidstream.application.auth.port.JwtPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtPort jwtPort;

  public JwtAuthenticationFilter(JwtPort jwtPort) {
    this.jwtPort = jwtPort;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      jwtPort
          .parseAccessToken(token)
          .ifPresent(
              claims -> {
                var authorities =
                    claims.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                var authentication =
                    new UsernamePasswordAuthenticationToken(claims.userId(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
              });
    }
    filterChain.doFilter(request, response);
  }
}
