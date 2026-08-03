package com.bidstream.application.outbox;

import com.bidstream.application.metrics.BidstreamMetrics;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxRelayService {

  private static final int BATCH_SIZE = 100;

  private final OutboxPort outboxPort;
  private final IntegrationEventPublisherPort publisher;
  private final BidstreamMetrics metrics;

  public OutboxRelayService(
      OutboxPort outboxPort, IntegrationEventPublisherPort publisher, BidstreamMetrics metrics) {
    this.outboxPort = outboxPort;
    this.publisher = publisher;
    this.metrics = metrics;
  }

  @Transactional
  public int relayBatch() {
    List<OutboxEvent> batch = outboxPort.claimUnpublished(BATCH_SIZE);
    int published = 0;
    for (OutboxEvent event : batch) {
      if (event.isFailed()) {
        continue;
      }
      try {
        long publishStart = System.nanoTime();
        publisher.publish(event);
        metrics.recordOutboxPublishLatency(Duration.ofNanos(System.nanoTime() - publishStart));
        outboxPort.markPublished(event.id());
        published++;
      } catch (RuntimeException ex) {
        outboxPort.recordFailure(event.id(), ex.getMessage());
      }
    }
    return published;
  }
}
