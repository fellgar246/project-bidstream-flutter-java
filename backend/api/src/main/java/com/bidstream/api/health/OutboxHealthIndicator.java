package com.bidstream.api.health;

import com.bidstream.application.outbox.OutboxPort;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class OutboxHealthIndicator implements HealthIndicator {

  private static final long PENDING_THRESHOLD = 1000;

  private final OutboxPort outboxPort;

  public OutboxHealthIndicator(OutboxPort outboxPort) {
    this.outboxPort = outboxPort;
  }

  @Override
  public Health health() {
    long pending = outboxPort.countPending();
    boolean stuck = outboxPort.hasStuckEvents();
    if (pending > PENDING_THRESHOLD || stuck) {
      return Health.down().withDetail("pending", pending).withDetail("stuckEvents", stuck).build();
    }
    return Health.up().withDetail("pending", pending).build();
  }
}
