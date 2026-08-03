package com.bidstream.application.auction;

import com.bidstream.application.cache.LotCacheInvalidator;
import com.bidstream.application.outbox.OutboxWriter;
import com.bidstream.application.realtime.DomainEventPublisher;
import com.bidstream.application.realtime.LotRealtimeEvent;
import com.bidstream.domain.bid.Bid;
import com.bidstream.domain.bid.BidRepository;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseAuctionService {

  public static final String REASON_NO_BIDS = "no_bids";
  public static final String REASON_RESERVE_NOT_MET = "reserve_not_met";

  private final LotRepository lotRepository;
  private final BidRepository bidRepository;
  private final OutboxWriter outboxWriter;
  private final DomainEventPublisher domainEventPublisher;
  private final Clock clock;
  private final LotCacheInvalidator lotCacheInvalidator;

  public CloseAuctionService(
      LotRepository lotRepository,
      BidRepository bidRepository,
      OutboxWriter outboxWriter,
      DomainEventPublisher domainEventPublisher,
      Clock clock,
      LotCacheInvalidator lotCacheInvalidator) {
    this.lotRepository = lotRepository;
    this.bidRepository = bidRepository;
    this.outboxWriter = outboxWriter;
    this.domainEventPublisher = domainEventPublisher;
    this.clock = clock;
    this.lotCacheInvalidator = lotCacheInvalidator;
  }

  @Transactional
  public Lot closeLot(long lotId) {
    Instant now = clock.instant();
    Lot lot =
        lotRepository
            .findByIdForUpdate(lotId)
            .orElseThrow(() -> new NoSuchElementException("Lot not found"));

    if (lot.status() != com.bidstream.domain.lot.LotStatus.LIVE) {
      return lot;
    }

    if (lot.bidCount() == 0) {
      Lot closed = lotRepository.save(lot.closeNoSale(now));
      outboxWriter.writeLotClosedNoSale(closed, REASON_NO_BIDS, now);
      domainEventPublisher.publish(
          new LotRealtimeEvent.LotClosedEvent(
              lotId, closed, Optional.empty(), REASON_NO_BIDS, now));
      lotCacheInvalidator.invalidateLotAndCatalog(lotId);
      return closed;
    }

    if (lot.hasReserve() && !lot.isReserveMet()) {
      Lot closed = lotRepository.save(lot.closeNoSale(now));
      outboxWriter.writeLotClosedNoSale(closed, REASON_RESERVE_NOT_MET, now);
      domainEventPublisher.publish(
          new LotRealtimeEvent.LotClosedEvent(
              lotId, closed, Optional.empty(), REASON_RESERVE_NOT_MET, now));
      lotCacheInvalidator.invalidateLotAndCatalog(lotId);
      return closed;
    }

    Bid winningBid =
        bidRepository
            .findHighestBidByLotId(lotId)
            .orElseThrow(() -> new IllegalStateException("Expected highest bid"));
    Lot closed = lotRepository.save(lot.closeSold(winningBid.id(), now));
    outboxWriter.writeLotClosedSold(
        closed, winningBid.id(), winningBid.bidderId(), winningBid.amount().cents(), now);
    domainEventPublisher.publish(
        new LotRealtimeEvent.LotClosedEvent(
            lotId, closed, Optional.of(winningBid.bidderId()), null, now));
    lotCacheInvalidator.invalidateLotAndCatalog(lotId);
    return closed;
  }
}
