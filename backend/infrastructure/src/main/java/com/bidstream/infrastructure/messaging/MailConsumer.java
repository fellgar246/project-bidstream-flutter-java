package com.bidstream.infrastructure.messaging;

import com.bidstream.application.messaging.MailPort;
import com.bidstream.application.messaging.ProcessedEventPort;
import com.bidstream.application.outbox.OutboxEvent;
import com.bidstream.domain.user.User;
import com.bidstream.domain.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MailConsumer {

  static final String CONSUMER_NAME = "mail";

  private final ProcessedEventPort processedEventPort;
  private final MailPort mailPort;
  private final UserRepository userRepository;
  private final Clock clock;

  public MailConsumer(
      ProcessedEventPort processedEventPort,
      MailPort mailPort,
      UserRepository userRepository,
      Clock clock) {
    this.processedEventPort = processedEventPort;
    this.mailPort = mailPort;
    this.userRepository = userRepository;
    this.clock = clock;
  }

  @RabbitListener(queues = RabbitMqConfig.MAIL_QUEUE)
  public void handle(IntegrationMessage message) {
    if (!OutboxEvent.LOT_CLOSED_SOLD.equals(message.eventType())) {
      return;
    }
    Instant now = clock.instant();
    if (processedEventPort.tryMarkProcessed(CONSUMER_NAME, message.eventId(), now)) {
      return;
    }
    Map<String, Object> payload = message.payload();
    long winnerId = ((Number) payload.get("winnerId")).longValue();
    long sellerId = ((Number) payload.get("sellerId")).longValue();
    long lotId = ((Number) payload.get("lotId")).longValue();
    long amountCents = ((Number) payload.get("amountCents")).longValue();
    String amount = String.format("$%.2f", amountCents / 100.0);

    userRepository
        .findById(winnerId)
        .ifPresent(
            user ->
                mailPort.send(
                    user.email(),
                    "You won lot #" + lotId,
                    "Congratulations! You won lot #" + lotId + " for " + amount + "."));

    userRepository
        .findById(sellerId)
        .ifPresent(
            user ->
                mailPort.send(
                    user.email(),
                    "Your lot #" + lotId + " sold",
                    "Lot #" + lotId + " closed with a winning bid of " + amount + "."));
  }
}
