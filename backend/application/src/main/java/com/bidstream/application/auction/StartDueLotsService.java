package com.bidstream.application.auction;

import com.bidstream.application.outbox.OutboxWriter;
import com.bidstream.application.realtime.DomainEventPublisher;
import com.bidstream.application.realtime.LotRealtimeEvent;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StartDueLotsService {

  private static final int BATCH_SIZE = 50;

  private final LotRepository lotRepository;
  private final OutboxWriter outboxWriter;
  private final DomainEventPublisher domainEventPublisher;
  private final Clock clock;

  public StartDueLotsService(
      LotRepository lotRepository,
      OutboxWriter outboxWriter,
      DomainEventPublisher domainEventPublisher,
      Clock clock) {
    this.lotRepository = lotRepository;
    this.outboxWriter = outboxWriter;
    this.domainEventPublisher = domainEventPublisher;
    this.clock = clock;
  }

  @Transactional
  public int startDueLots() {
    Instant now = clock.instant();
    List<Lot> due = lotRepository.findScheduledReadyToStart(now, BATCH_SIZE);
    int started = 0;
    for (Lot lot : due) {
      Lot live = lotRepository.save(lot.start(now));
      outboxWriter.writeLotStarted(live, now);
      domainEventPublisher.publish(new LotRealtimeEvent.LotStartedEvent(lot.id(), live, now));
      started++;
    }
    return started;
  }
}
