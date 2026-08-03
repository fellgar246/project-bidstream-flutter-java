package com.bidstream.api.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.bidstream.infrastructure.messaging.IntegrationMessage;
import com.bidstream.infrastructure.messaging.NotificationConsumer;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
class IdempotentConsumerIT {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private NotificationConsumer notificationConsumer;

  private long leaderId;

  @BeforeEach
  void seedUser() {
    leaderId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO users (email, password_hash, display_name, created_at, updated_at)
            VALUES (?, 'hash', 'Leader', NOW(), NOW())
            RETURNING id
            """,
            Long.class,
            "leader-" + System.nanoTime() + "@test.com");
  }

  @Test
  void ca077_redeliveryCreatesSingleNotification() {
    long eventId = insertOutboxEvent();
    IntegrationMessage message =
        new IntegrationMessage(
            eventId,
            "BidPlaced",
            "Lot",
            1L,
            Instant.now().toString(),
            Map.of(
                "lotId", 1L,
                "bidId", 10L,
                "bidderId", 2L,
                "amountCents", 10000L,
                "previousLeaderId", leaderId));

    for (int i = 0; i < 5; i++) {
      notificationConsumer.handle(message);
    }

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ?", Integer.class, leaderId))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE consumer = ? AND event_id = ?",
                Integer.class,
                NotificationConsumer.CONSUMER_NAME,
                eventId))
        .isEqualTo(1);
  }

  private long insertOutboxEvent() {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload, occurred_at, attempts)
        VALUES ('Lot', 1, 'BidPlaced', '{"lotId":1}'::jsonb, NOW(), 0)
        RETURNING id
        """,
        Long.class);
  }
}
