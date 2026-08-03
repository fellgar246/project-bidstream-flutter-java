package com.bidstream.api.push;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidstream.api.support.IntegrationTestInitializer;
import com.bidstream.application.push.PushPort;
import com.bidstream.infrastructure.messaging.IntegrationMessage;
import com.bidstream.infrastructure.messaging.PushConsumer;
import com.bidstream.infrastructure.push.NoOpPushAdapter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
class PushConsumerIT {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PushConsumer pushConsumer;
  @Autowired private PushPort pushPort;

  private long leaderId;

  @BeforeEach
  void seed() {
    NoOpPushAdapter adapter = (NoOpPushAdapter) pushPort;
    adapter.clear();
    leaderId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO users (email, password_hash, display_name, created_at, updated_at)
            VALUES (?, 'hash', 'Leader', NOW(), NOW())
            RETURNING id
            """,
            Long.class,
            "push-leader-" + System.nanoTime() + "@test.com");

    jdbcTemplate.update(
        """
        INSERT INTO device_tokens (user_id, token, platform, last_seen_at, created_at)
        VALUES (?, 'test-fcm-token', 'android', NOW(), NOW())
        """,
        leaderId);
  }

  @Test
  void idempotentRedeliverySendsOnce() {
    long eventId = insertOutboxEvent();
    IntegrationMessage message = outbidMessage(eventId);

    for (int i = 0; i < 5; i++) {
      pushConsumer.handle(message);
    }

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE consumer = ? AND event_id = ?",
                Integer.class,
                PushConsumer.CONSUMER_NAME,
                eventId))
        .isEqualTo(1);

    NoOpPushAdapter adapter = (NoOpPushAdapter) pushPort;
    assertThat(adapter.sentPushes()).hasSize(1);
    assertThat(adapter.sentPushes().getFirst().message().data().get("deepLink"))
        .isEqualTo("bidstream://lots/1");
  }

  @Test
  void invalidTokenIsRemoved() {
    jdbcTemplate.update("DELETE FROM device_tokens WHERE token = 'test-fcm-token'");
    jdbcTemplate.update(
        """
        INSERT INTO device_tokens (user_id, token, platform, last_seen_at, created_at)
        VALUES (?, 'invalid-token', 'android', NOW(), NOW())
        """,
        leaderId);

    NoOpPushAdapter adapter = (NoOpPushAdapter) pushPort;
    adapter.setInvalidTokens(List.of("invalid-token"));

    pushConsumer.handle(outbidMessage(insertOutboxEvent()));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM device_tokens WHERE token = ?", Integer.class, "invalid-token"))
        .isZero();
  }

  private IntegrationMessage outbidMessage(long eventId) {
    return new IntegrationMessage(
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
