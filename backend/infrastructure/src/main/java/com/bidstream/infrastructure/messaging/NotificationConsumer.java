package com.bidstream.infrastructure.messaging;

import com.bidstream.application.messaging.ProcessedEventPort;
import com.bidstream.application.notification.Notification;
import com.bidstream.application.notification.NotificationRepository;
import com.bidstream.application.notification.UserNotificationPushPort;
import com.bidstream.application.outbox.OutboxEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

  public static final String CONSUMER_NAME = "notifications";

  private final ProcessedEventPort processedEventPort;
  private final NotificationRepository notificationRepository;
  private final UserNotificationPushPort pushPort;
  private final Clock clock;

  public NotificationConsumer(
      ProcessedEventPort processedEventPort,
      NotificationRepository notificationRepository,
      UserNotificationPushPort pushPort,
      Clock clock) {
    this.processedEventPort = processedEventPort;
    this.notificationRepository = notificationRepository;
    this.pushPort = pushPort;
    this.clock = clock;
  }

  @RabbitListener(queues = RabbitMqConfig.NOTIFICATIONS_QUEUE)
  public void handle(IntegrationMessage message) {
    Instant now = clock.instant();
    if (processedEventPort.tryMarkProcessed(CONSUMER_NAME, message.eventId(), now)) {
      return;
    }
    for (NotificationCandidate candidate : candidatesFor(message)) {
      Notification saved = notificationRepository.save(candidate.toNotification(now));
      pushPort.push(candidate.userId(), saved);
    }
  }

  private List<NotificationCandidate> candidatesFor(IntegrationMessage message) {
    Map<String, Object> payload = message.payload();
    List<NotificationCandidate> result = new ArrayList<>();
    switch (message.eventType()) {
      case OutboxEvent.BID_PLACED -> {
        Object previousLeader = payload.get("previousLeaderId");
        if (previousLeader instanceof Number leaderId) {
          result.add(
              new NotificationCandidate(
                  leaderId.longValue(),
                  "OUTBID",
                  Map.of(
                      "lotId", payload.get("lotId"),
                      "amountCents", payload.get("amountCents"))));
        }
      }
      case OutboxEvent.LOT_STARTED ->
          result.add(
              new NotificationCandidate(
                  ((Number) payload.get("sellerId")).longValue(),
                  "LOT_STARTED",
                  Map.copyOf(payload)));
      case OutboxEvent.LOT_CLOSED_SOLD -> {
        result.add(
            new NotificationCandidate(
                ((Number) payload.get("winnerId")).longValue(), "YOU_WON", Map.copyOf(payload)));
        result.add(
            new NotificationCandidate(
                ((Number) payload.get("sellerId")).longValue(), "LOT_SOLD", Map.copyOf(payload)));
      }
      case OutboxEvent.LOT_CLOSED_NO_SALE ->
          result.add(
              new NotificationCandidate(
                  ((Number) payload.get("sellerId")).longValue(),
                  "LOT_NO_SALE",
                  Map.copyOf(payload)));
      default -> {}
    }
    return result;
  }

  private record NotificationCandidate(long userId, String type, Map<String, Object> payload) {
    Notification toNotification(Instant createdAt) {
      return new Notification(0L, userId, type, new LinkedHashMap<>(payload), null, createdAt);
    }
  }
}
