package com.bidstream.api.realtime.auth;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
      return message;
    }
    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
    if (sessionAttributes == null || !sessionAttributes.containsKey("userId")) {
      return message;
    }
    long userId = (long) sessionAttributes.get("userId");
    @SuppressWarnings("unchecked")
    List<String> roles = (List<String>) sessionAttributes.get("roles");
    accessor.setUser(new StompPrincipal(userId, roles));
    return message;
  }

  public record StompPrincipal(long userId, List<String> roles) implements Principal {

    @Override
    public String getName() {
      return String.valueOf(userId);
    }
  }
}
