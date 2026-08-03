package com.bidstream.api.metrics;

import com.bidstream.application.outbox.OutboxPort;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsGaugeConfig {

  public MetricsGaugeConfig(
      MeterRegistry registry,
      LotRepository lotRepository,
      OutboxPort outboxPort,
      WebSocketSessionTracker sessionTracker) {
    Gauge.builder("bidstream.lots.live", lotRepository, repo -> repo.countByStatus(LotStatus.LIVE))
        .description("Number of lots currently in LIVE status")
        .register(registry);
    Gauge.builder("bidstream.outbox.pending", outboxPort, OutboxPort::countPending)
        .description("Unpublished outbox events")
        .register(registry);
    Gauge.builder("bidstream.ws.sessions", sessionTracker, WebSocketSessionTracker::activeCount)
        .description("Active WebSocket sessions")
        .register(registry);
  }
}
