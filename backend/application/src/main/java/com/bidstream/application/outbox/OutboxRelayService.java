package com.bidstream.application.outbox;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxRelayService {

  private static final int BATCH_SIZE = 100;

  private final OutboxPort outboxPort;
  private final IntegrationEventPublisherPort publisher;

  public OutboxRelayService(OutboxPort outboxPort, IntegrationEventPublisherPort publisher) {
    this.outboxPort = outboxPort;
    this.publisher = publisher;
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
        publisher.publish(event);
        outboxPort.markPublished(event.id());
        published++;
      } catch (RuntimeException ex) {
        outboxPort.recordFailure(event.id(), ex.getMessage());
      }
    }
    return published;
  }
}
