package com.bidstream.api.realtime;

import com.bidstream.application.realtime.LotEventEnvelope;
import com.bidstream.application.realtime.LotPresencePort;
import com.bidstream.application.realtime.LotRealtimeEvent;
import com.bidstream.domain.money.Money;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RealtimeBroadcastListener {

  private final SimpMessagingTemplate messagingTemplate;
  private final LotEventMapper lotEventMapper;
  private final LotPresencePort lotPresencePort;
  private final PresenceRateLimiter presenceRateLimiter;

  public RealtimeBroadcastListener(
      SimpMessagingTemplate messagingTemplate,
      LotEventMapper lotEventMapper,
      LotPresencePort lotPresencePort,
      PresenceRateLimiter presenceRateLimiter) {
    this.messagingTemplate = messagingTemplate;
    this.lotEventMapper = lotEventMapper;
    this.lotPresencePort = lotPresencePort;
    this.presenceRateLimiter = presenceRateLimiter;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onBidPlaced(LotRealtimeEvent.BidPlacedEvent event) {
    LotEventEnvelope envelope = lotEventMapper.toBidPlaced(event);
    messagingTemplate.convertAndSend("/topic/lots/" + event.lotId(), envelope);

    event
        .previousHighestBidderId()
        .ifPresent(
            outbidUserId -> {
              Map<String, Object> outbid = new LinkedHashMap<>();
              outbid.put("type", "OUTBID");
              outbid.put("lotId", event.lotId());
              outbid.put(
                  "yourAmount", event.previousHighestAmount().map(Money::toString).orElse("0.00"));
              outbid.put("newAmount", event.lot().currentPrice().toString());
              messagingTemplate.convertAndSendToUser(
                  String.valueOf(outbidUserId), "/queue/bids", outbid);
            });
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onLotExtended(LotRealtimeEvent.LotExtendedEvent event) {
    LotEventEnvelope envelope = lotEventMapper.toLotExtended(event);
    messagingTemplate.convertAndSend("/topic/lots/" + event.lotId(), envelope);
  }

  public void publishPresence(long lotId) {
    if (!presenceRateLimiter.shouldPublish(lotId)) {
      return;
    }
    int watching = lotPresencePort.countWatching(lotId);
    messagingTemplate.convertAndSend(
        "/topic/lots/" + lotId + "/presence", Map.of("watching", watching));
  }

  @Component
  static class PresenceRateLimiter {

    private static final Duration MIN_INTERVAL = Duration.ofSeconds(2);

    private final ConcurrentHashMap<Long, Long> lastPublishedMillis = new ConcurrentHashMap<>();

    boolean shouldPublish(long lotId) {
      long now = System.currentTimeMillis();
      Long previous = lastPublishedMillis.get(lotId);
      if (previous != null && now - previous < MIN_INTERVAL.toMillis()) {
        return false;
      }
      lastPublishedMillis.put(lotId, now);
      return true;
    }
  }
}
