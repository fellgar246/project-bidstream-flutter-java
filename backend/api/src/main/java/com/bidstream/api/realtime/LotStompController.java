package com.bidstream.api.realtime;

import com.bidstream.application.realtime.LotPresencePort;
import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
public class LotStompController {

  private final LotPresencePort lotPresencePort;
  private final RealtimeBroadcastListener broadcastListener;
  private final SessionLotSubscriptions sessionLotSubscriptions;

  public LotStompController(
      LotPresencePort lotPresencePort,
      RealtimeBroadcastListener broadcastListener,
      SessionLotSubscriptions sessionLotSubscriptions) {
    this.lotPresencePort = lotPresencePort;
    this.broadcastListener = broadcastListener;
    this.sessionLotSubscriptions = sessionLotSubscriptions;
  }

  @MessageMapping("/lots/{lotId}/subscribe")
  public void subscribe(
      @DestinationVariable long lotId, Principal principal, SimpMessageHeaderAccessor accessor) {
    long userId = Long.parseLong(principal.getName());
    lotPresencePort.markPresent(lotId, userId);
    sessionLotSubscriptions.track(accessor.getSessionId(), userId, lotId);
    broadcastListener.publishPresence(lotId);
  }

  @MessageMapping("/lots/{lotId}/unsubscribe")
  public void unsubscribe(
      @DestinationVariable long lotId, Principal principal, SimpMessageHeaderAccessor accessor) {
    long userId = Long.parseLong(principal.getName());
    lotPresencePort.markAbsent(lotId, userId);
    sessionLotSubscriptions.untrack(accessor.getSessionId(), lotId);
    broadcastListener.publishPresence(lotId);
  }

  @EventListener
  public void onDisconnect(SessionDisconnectEvent event) {
    String sessionId = event.getSessionId();
    Long userId = sessionLotSubscriptions.userIdForSession(sessionId);
    if (userId == null) {
      return;
    }
    for (long lotId : sessionLotSubscriptions.clearSession(sessionId)) {
      lotPresencePort.markAbsent(lotId, userId);
      broadcastListener.publishPresence(lotId);
    }
  }

  @Component
  static class SessionLotSubscriptions {

    private final ConcurrentHashMap<String, Long> sessionUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Long>> sessionLots = new ConcurrentHashMap<>();

    void track(String sessionId, long userId, long lotId) {
      sessionUsers.putIfAbsent(sessionId, userId);
      sessionLots.computeIfAbsent(sessionId, ignored -> ConcurrentHashMap.newKeySet()).add(lotId);
    }

    void untrack(String sessionId, long lotId) {
      Set<Long> lots = sessionLots.get(sessionId);
      if (lots != null) {
        lots.remove(lotId);
      }
    }

    Set<Long> clearSession(String sessionId) {
      Set<Long> lots = sessionLots.remove(sessionId);
      sessionUsers.remove(sessionId);
      return lots == null ? Set.of() : Set.copyOf(lots);
    }

    Long userIdForSession(String sessionId) {
      return sessionUsers.get(sessionId);
    }
  }
}
