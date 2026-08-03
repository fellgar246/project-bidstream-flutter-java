package com.bidstream.application.bid;

import com.bidstream.application.realtime.DomainEventPublisher;
import com.bidstream.application.realtime.LotRealtimeEvent;
import com.bidstream.domain.bid.Bid;
import com.bidstream.domain.bid.BidConflictException;
import com.bidstream.domain.bid.BidRepository;
import com.bidstream.domain.bid.LockTimeoutException;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PlaceBidService {

  private static final int MAX_RETRIES = 3;
  private static final String LOCK_PREFIX = "lock:lot:";

  private final LotRepository lotRepository;
  private final BidRepository bidRepository;
  private final BidValidator bidValidator;
  private final DistributedLockPort distributedLock;
  private final TransactionTemplate transactionTemplate;
  private final DomainEventPublisher domainEventPublisher;
  private final UserRepository userRepository;
  private final Clock clock;

  public PlaceBidService(
      LotRepository lotRepository,
      BidRepository bidRepository,
      BidValidator bidValidator,
      DistributedLockPort distributedLock,
      @org.springframework.beans.factory.annotation.Qualifier("requiresNewTransactionTemplate")
          TransactionTemplate transactionTemplate,
      DomainEventPublisher domainEventPublisher,
      UserRepository userRepository,
      Clock clock) {
    this.lotRepository = lotRepository;
    this.bidRepository = bidRepository;
    this.bidValidator = bidValidator;
    this.distributedLock = distributedLock;
    this.transactionTemplate = transactionTemplate;
    this.domainEventPublisher = domainEventPublisher;
    this.userRepository = userRepository;
    this.clock = clock;
  }

  public PlaceBidOutcome placeBid(long lotId, long bidderId, Money amount, String clientRequestId) {
    Optional<DistributedLockPort.LockToken> lock =
        distributedLock.tryAcquire(
            LOCK_PREFIX + lotId,
            DistributedLockPort.DEFAULT_TTL,
            DistributedLockPort.DEFAULT_MAX_WAIT);
    if (lock.isEmpty()) {
      throw new LockTimeoutException();
    }
    try {
      for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
        try {
          PlaceBidOutcome outcome =
              transactionTemplate.execute(
                  status -> attemptPlaceBid(lotId, bidderId, amount, clientRequestId));
          if (outcome != null) {
            return outcome;
          }
        } catch (OptimisticLockingFailureException ex) {
          // retry with fresh state
        } catch (DataIntegrityViolationException ex) {
          return handleDuplicateBid(lotId, bidderId, clientRequestId);
        }
      }
      throw new BidConflictException();
    } finally {
      distributedLock.release(lock.get());
    }
  }

  private PlaceBidOutcome attemptPlaceBid(
      long lotId, long bidderId, Money amount, String clientRequestId) {
    Instant now = clock.instant();
    Lot lot =
        lotRepository
            .findById(lotId)
            .orElseThrow(() -> new NoSuchElementException("Lot not found"));

    Optional<Bid> existing =
        bidRepository.findByLotIdAndBidderIdAndClientRequestId(lotId, bidderId, clientRequestId);
    if (existing.isPresent()) {
      return PlaceBidOutcome.replay(existing.get(), lot);
    }

    bidValidator.validate(lot, bidderId, amount, now);

    Optional<Bid> previousHighest =
        lot.bidCount() > 0 ? bidRepository.findHighestBidByLotId(lotId) : Optional.empty();
    Optional<Long> previousHighestBidderId =
        previousHighest.filter(bid -> bid.bidderId() != bidderId).map(Bid::bidderId);
    Optional<Money> previousHighestAmount = previousHighest.map(bid -> bid.amount());

    Bid bid = bidRepository.save(Bid.create(lotId, bidderId, amount, now, clientRequestId));
    Lot.BidAcceptanceResult acceptance = lot.acceptBid(amount, now);
    Lot savedLot = lotRepository.save(acceptance.lot());

    String bidderDisplayName =
        userRepository.findById(bidderId).map(user -> user.displayName()).orElse("Bidder");

    domainEventPublisher.publish(
        new LotRealtimeEvent.BidPlacedEvent(
            lotId,
            bid,
            savedLot,
            bidderDisplayName,
            acceptance.extended(),
            previousHighestBidderId,
            previousHighestAmount,
            now));
    if (acceptance.extended()) {
      domainEventPublisher.publish(
          new LotRealtimeEvent.LotExtendedEvent(lotId, savedLot, bid.id(), now));
    }

    // TODO(SPEC-07): write outbox event in same transaction (RB-09)

    return PlaceBidOutcome.created(bid, savedLot, acceptance.extended());
  }

  private PlaceBidOutcome handleDuplicateBid(long lotId, long bidderId, String clientRequestId) {
    Bid existing =
        bidRepository
            .findByLotIdAndBidderIdAndClientRequestId(lotId, bidderId, clientRequestId)
            .orElseThrow(() -> new BidConflictException());
    Lot lot = lotRepository.findById(lotId).orElseThrow();
    return PlaceBidOutcome.replay(existing, lot);
  }

  public record PlaceBidOutcome(PlaceBidResult result, boolean idempotentReplay) {

    static PlaceBidOutcome created(Bid bid, Lot lot, boolean extended) {
      return new PlaceBidOutcome(
          PlaceBidResult.of(bid, lot, bid.bidderId(), extended, false), false);
    }

    static PlaceBidOutcome replay(Bid bid, Lot lot) {
      return new PlaceBidOutcome(PlaceBidResult.of(bid, lot, bid.bidderId(), false, true), true);
    }
  }
}
