package com.bidstream.infrastructure.messaging;

import com.bidstream.application.device.DeviceToken;
import com.bidstream.application.device.DeviceTokenRepository;
import com.bidstream.application.messaging.ProcessedEventPort;
import com.bidstream.application.outbox.OutboxEvent;
import com.bidstream.application.push.PushPort;
import com.bidstream.domain.lot.WatchRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PushConsumer {

  public static final String CONSUMER_NAME = "push";

  private final ProcessedEventPort processedEventPort;
  private final DeviceTokenRepository deviceTokenRepository;
  private final WatchRepository watchRepository;
  private final PushPort pushPort;
  private final Clock clock;

  public PushConsumer(
      ProcessedEventPort processedEventPort,
      DeviceTokenRepository deviceTokenRepository,
      WatchRepository watchRepository,
      PushPort pushPort,
      Clock clock) {
    this.processedEventPort = processedEventPort;
    this.deviceTokenRepository = deviceTokenRepository;
    this.watchRepository = watchRepository;
    this.pushPort = pushPort;
    this.clock = clock;
  }

  @RabbitListener(queues = RabbitMqConfig.PUSH_QUEUE)
  @Transactional
  public void handle(IntegrationMessage message) {
    Instant now = clock.instant();
    if (processedEventPort.tryMarkProcessed(CONSUMER_NAME, message.eventId(), now)) {
      return;
    }
    for (PushTarget target : targetsFor(message)) {
      sendPush(target);
    }
  }

  private void sendPush(PushTarget target) {
    List<DeviceToken> tokens = deviceTokenRepository.findByUserId(target.userId());
    if (tokens.isEmpty()) {
      return;
    }
    List<String> tokenStrings = tokens.stream().map(DeviceToken::token).toList();
    Map<String, String> data = new LinkedHashMap<>(target.data());
    data.put("type", target.type());
    data.put("deepLink", target.deepLink());

    PushPort.PushMessage pushMessage =
        new PushPort.PushMessage(target.title(), target.body(), data);
    PushPort.PushResult result = pushPort.send(pushMessage, tokenStrings);
    for (String invalid : result.invalidTokens()) {
      deviceTokenRepository.deleteByToken(invalid);
    }
  }

  private List<PushTarget> targetsFor(IntegrationMessage message) {
    Map<String, Object> payload = message.payload();
    List<PushTarget> result = new ArrayList<>();
    switch (message.eventType()) {
      case OutboxEvent.BID_PLACED -> {
        Object previousLeader = payload.get("previousLeaderId");
        if (previousLeader instanceof Number leaderId) {
          long lotId = ((Number) payload.get("lotId")).longValue();
          result.add(
              new PushTarget(
                  leaderId.longValue(),
                  "OUTBID",
                  "Outbid",
                  "You were outbid on a lot",
                  lotId));
        }
      }
      case OutboxEvent.LOT_STARTED -> {
        long lotId = ((Number) payload.get("lotId")).longValue();
        for (long watcherId : watchRepository.findUserIdsByLotId(lotId)) {
          result.add(
              new PushTarget(
                  watcherId,
                  "LOT_STARTED",
                  "Lot started",
                  "A lot you watch is now live",
                  lotId));
        }
      }
      case OutboxEvent.LOT_CLOSED_SOLD -> {
        long lotId = ((Number) payload.get("lotId")).longValue();
        result.add(
            new PushTarget(
                ((Number) payload.get("winnerId")).longValue(),
                "LOT_CLOSED",
                "You won!",
                "Your winning lot has closed",
                lotId));
        result.add(
            new PushTarget(
                ((Number) payload.get("sellerId")).longValue(),
                "LOT_CLOSED",
                "Lot sold",
                "Your lot sold at auction",
                lotId));
      }
      case OutboxEvent.LOT_CLOSED_NO_SALE -> {
        long lotId = ((Number) payload.get("lotId")).longValue();
        result.add(
            new PushTarget(
                ((Number) payload.get("sellerId")).longValue(),
                "LOT_CLOSED",
                "Lot closed",
                "Your lot closed without sale",
                lotId));
      }
      default -> {}
    }
    return result;
  }

  private record PushTarget(
      long userId, String type, String title, String body, long lotId) {

    String deepLink() {
      return "bidstream://lots/" + lotId;
    }

    Map<String, String> data() {
      return Map.of("lotId", String.valueOf(lotId));
    }
  }
}
