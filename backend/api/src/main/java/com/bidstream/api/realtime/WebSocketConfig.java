package com.bidstream.api.realtime;

import com.bidstream.api.realtime.auth.StompAuthChannelInterceptor;
import com.bidstream.api.realtime.auth.StompAuthHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * STOMP over native WebSocket at {@code /ws}.
 *
 * <p>Auth uses {@code ?token=<accessToken>} on the handshake URL because browsers and mobile
 * WebSocket clients cannot set arbitrary headers (e.g. {@code Authorization}) during the upgrade.
 * Tokens may appear in proxy access logs; mitigated by the 15-minute access-token TTL.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompAuthHandshakeInterceptor handshakeInterceptor;
  private final StompAuthChannelInterceptor channelInterceptor;
  private final ThreadPoolTaskScheduler wsHeartbeatScheduler;

  public WebSocketConfig(
      StompAuthHandshakeInterceptor handshakeInterceptor,
      StompAuthChannelInterceptor channelInterceptor,
      @Qualifier("wsHeartbeatScheduler") ThreadPoolTaskScheduler wsHeartbeatScheduler) {
    this.handshakeInterceptor = handshakeInterceptor;
    this.channelInterceptor = channelInterceptor;
    this.wsHeartbeatScheduler = wsHeartbeatScheduler;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry
        .enableSimpleBroker("/topic", "/queue")
        .setHeartbeatValue(new long[] {10_000L, 10_000L})
        .setTaskScheduler(wsHeartbeatScheduler);
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .addInterceptors(handshakeInterceptor)
        .setHandshakeHandler(
            new DefaultHandshakeHandler() {
              @Override
              protected java.security.Principal determineUser(
                  ServerHttpRequest request,
                  WebSocketHandler wsHandler,
                  java.util.Map<String, Object> attributes) {
                Object userId = attributes.get("userId");
                if (userId == null) {
                  return null;
                }
                String name = String.valueOf(userId);
                return () -> name;
              }
            })
        .setAllowedOriginPatterns("*");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(channelInterceptor);
  }
}
