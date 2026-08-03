package com.bidstream.api.metrics;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketSessionTracker {

  private final AtomicInteger activeSessions = new AtomicInteger();

  @EventListener
  public void onConnect(SessionConnectEvent event) {
    activeSessions.incrementAndGet();
  }

  @EventListener
  public void onDisconnect(SessionDisconnectEvent event) {
    activeSessions.updateAndGet(current -> Math.max(0, current - 1));
  }

  public int activeCount() {
    return activeSessions.get();
  }
}
