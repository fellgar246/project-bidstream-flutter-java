package com.bidstream.api.realtime.auth;

import com.bidstream.application.auth.port.JwtPort;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class StompAuthHandshakeInterceptor implements HandshakeInterceptor {

  private final JwtPort jwtPort;

  public StompAuthHandshakeInterceptor(JwtPort jwtPort) {
    this.jwtPort = jwtPort;
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    String token = extractToken(request);
    if (token == null || token.isBlank()) {
      reject(response);
      return false;
    }
    var claims = jwtPort.parseAccessToken(token);
    if (claims.isEmpty()) {
      reject(response);
      return false;
    }
    JwtPort.AccessTokenClaims parsed = claims.get();
    attributes.put("userId", parsed.userId());
    attributes.put("email", parsed.email());
    attributes.put("roles", parsed.roles());
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // no-op
  }

  private static void reject(ServerHttpResponse response) {
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
  }

  static String extractToken(ServerHttpRequest request) {
    if (request instanceof ServletServerHttpRequest servletRequest) {
      return servletRequest.getServletRequest().getParameter("token");
    }
    String query = request.getURI().getQuery();
    if (query == null) {
      return null;
    }
    for (String part : query.split("&")) {
      if (part.startsWith("token=")) {
        return part.substring("token=".length());
      }
    }
    return null;
  }
}
