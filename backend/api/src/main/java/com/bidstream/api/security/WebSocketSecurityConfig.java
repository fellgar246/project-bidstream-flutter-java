package com.bidstream.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * WebSocket handshake is authenticated by {@code StompAuthHandshakeInterceptor}, not JWT filter.
 */
@Configuration
@EnableWebSecurity
public class WebSocketSecurityConfig {

  @Bean
  @Order(0)
  SecurityFilterChain webSocketSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/ws", "/ws/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }
}
