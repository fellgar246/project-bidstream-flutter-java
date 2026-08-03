package com.bidstream.application.bid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidstream.application.realtime.DomainEventPublisher;
import com.bidstream.application.realtime.LotRealtimeEvent;
import com.bidstream.domain.bid.Bid;
import com.bidstream.domain.bid.BidConflictException;
import com.bidstream.domain.bid.BidNotLiveException;
import com.bidstream.domain.bid.BidRepository;
import com.bidstream.domain.bid.BidSelfException;
import com.bidstream.domain.bid.BidTooLowException;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.Role;
import com.bidstream.domain.user.User;
import com.bidstream.domain.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class PlaceBidServiceTest {

  private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

  @Mock private LotRepository lotRepository;
  @Mock private BidRepository bidRepository;
  @Mock private BidValidator bidValidator;
  @Mock private DistributedLockPort distributedLock;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private DomainEventPublisher domainEventPublisher;
  @Mock private com.bidstream.application.outbox.OutboxWriter outboxWriter;
  @Mock private UserRepository userRepository;

  @Mock private com.bidstream.application.cache.LotCacheInvalidator lotCacheInvalidator;

  private PlaceBidService service;
  private Lot liveLot;

  @BeforeEach
  void setUp() {
    service =
        new PlaceBidService(
            lotRepository,
            bidRepository,
            bidValidator,
            distributedLock,
            transactionTemplate,
            domainEventPublisher,
            outboxWriter,
            userRepository,
            Clock.fixed(NOW, ZoneOffset.UTC),
            lotCacheInvalidator);

    liveLot = liveLot(Money.fromCents(10000), 0);

    when(distributedLock.tryAcquire(any(), any(), any()))
        .thenReturn(Optional.of(new DistributedLockPort.LockToken("lock:lot:1", "tok")));
  }

  private void stubSuccessfulTransaction() {
    when(transactionTemplate.execute(any()))
        .thenAnswer(
            inv -> {
              TransactionCallback<?> cb = inv.getArgument(0);
              return cb.doInTransaction(new SimpleTransactionStatus());
            });
  }

  @Test
  void rb04_bidderIsSeller_isRejected() {
    stubSuccessfulTransaction();
    when(lotRepository.findById(1L)).thenReturn(Optional.of(liveLot));
    when(bidRepository.findByLotIdAndBidderIdAndClientRequestId(1L, 10L, "req-1"))
        .thenReturn(Optional.empty());
    doThrow(new BidSelfException())
        .when(bidValidator)
        .validate(liveLot, 10L, Money.fromString("105.00"), NOW);

    assertThatThrownBy(() -> service.placeBid(1L, 10L, Money.fromString("105.00"), "req-1"))
        .isInstanceOf(BidSelfException.class);
    verify(bidRepository, never()).save(any());
  }

  @Test
  void rb04_lotNotLive_isRejected() {
    stubSuccessfulTransaction();
    Lot scheduled = liveLot.withVersion(0).forceStatus(LotStatus.SCHEDULED, NOW);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(scheduled));
    when(bidRepository.findByLotIdAndBidderIdAndClientRequestId(1L, 2L, "req-1"))
        .thenReturn(Optional.empty());
    doThrow(new BidNotLiveException())
        .when(bidValidator)
        .validate(scheduled, 2L, Money.fromString("100.00"), NOW);

    assertThatThrownBy(() -> service.placeBid(1L, 2L, Money.fromString("100.00"), "req-1"))
        .isInstanceOf(BidNotLiveException.class);
  }

  @Test
  void rb04_bidTooLow_isRejected() {
    stubSuccessfulTransaction();
    when(lotRepository.findById(1L)).thenReturn(Optional.of(liveLot));
    when(bidRepository.findByLotIdAndBidderIdAndClientRequestId(1L, 2L, "req-1"))
        .thenReturn(Optional.empty());
    doThrow(new BidTooLowException(Money.fromString("100.00")))
        .when(bidValidator)
        .validate(liveLot, 2L, Money.fromString("50.00"), NOW);

    assertThatThrownBy(() -> service.placeBid(1L, 2L, Money.fromString("50.00"), "req-1"))
        .isInstanceOf(BidTooLowException.class);
  }

  @Test
  void rb04_firstBid_acceptsStartingPrice() {
    stubSuccessfulTransaction();
    Bid saved = Bid.create(1L, 2L, Money.fromString("100.00"), NOW, "req-1").withId(1L);
    Lot updated = liveLot.acceptBid(Money.fromString("100.00"), NOW).lot();

    when(lotRepository.findById(1L)).thenReturn(Optional.of(liveLot));
    when(bidRepository.findByLotIdAndBidderIdAndClientRequestId(1L, 2L, "req-1"))
        .thenReturn(Optional.empty());
    when(bidRepository.save(any())).thenReturn(saved);
    when(lotRepository.save(any())).thenReturn(updated);
    when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));

    var outcome = service.placeBid(1L, 2L, Money.fromString("100.00"), "req-1");

    assertThat(outcome.idempotentReplay()).isFalse();
    assertThat(outcome.result().bid().id()).isEqualTo(1L);
    assertThat(outcome.result().lot().currentPrice()).isEqualTo(Money.fromString("100.00"));
    assertThat(outcome.result().lot().bidCount()).isEqualTo(1);
    verify(domainEventPublisher).publish(any(LotRealtimeEvent.BidPlacedEvent.class));
  }

  @Test
  void antiSniping_publishesLotExtendedEvent() {
    stubSuccessfulTransaction();
    Instant end = NOW.plusSeconds(10);
    Lot snipeLot =
        new Lot(
            1L,
            10L,
            "Title",
            "Desc",
            1L,
            Money.fromString("100.00"),
            Money.fromString("5.00"),
            null,
            LotStatus.LIVE,
            NOW.minusSeconds(3600),
            end,
            null,
            Money.fromCents(0),
            0,
            null,
            0,
            0L,
            NOW,
            NOW);
    Bid saved = Bid.create(1L, 2L, Money.fromString("100.00"), NOW, "req-snipe").withId(1L);
    Lot updated = snipeLot.acceptBid(Money.fromString("100.00"), NOW).lot();

    when(lotRepository.findById(1L)).thenReturn(Optional.of(snipeLot));
    when(bidRepository.findByLotIdAndBidderIdAndClientRequestId(1L, 2L, "req-snipe"))
        .thenReturn(Optional.empty());
    when(bidRepository.save(any())).thenReturn(saved);
    when(lotRepository.save(any())).thenReturn(updated);
    when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));

    service.placeBid(1L, 2L, Money.fromString("100.00"), "req-snipe");

    verify(domainEventPublisher).publish(any(LotRealtimeEvent.BidPlacedEvent.class));
    verify(domainEventPublisher).publish(any(LotRealtimeEvent.LotExtendedEvent.class));
  }

  @Test
  void secondBid_publishesWithPreviousHighestBidder() {
    stubSuccessfulTransaction();
    Lot withOneBid = liveLot(Money.fromString("100.00"), 1);
    Bid previous = Bid.create(1L, 3L, Money.fromString("100.00"), NOW, "req-prev").withId(9L);
    Bid saved = Bid.create(1L, 2L, Money.fromString("105.00"), NOW, "req-2").withId(10L);
    Lot updated = withOneBid.acceptBid(Money.fromString("105.00"), NOW).lot();

    when(lotRepository.findById(1L)).thenReturn(Optional.of(withOneBid));
    when(bidRepository.findByLotIdAndBidderIdAndClientRequestId(1L, 2L, "req-2"))
        .thenReturn(Optional.empty());
    when(bidRepository.findHighestBidByLotId(1L)).thenReturn(Optional.of(previous));
    when(bidRepository.save(any())).thenReturn(saved);
    when(lotRepository.save(any())).thenReturn(updated);
    when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));

    service.placeBid(1L, 2L, Money.fromString("105.00"), "req-2");

    verify(domainEventPublisher).publish(any(LotRealtimeEvent.BidPlacedEvent.class));
  }

  @Test
  void idempotentReplay_doesNotPublishEvents() {
    stubSuccessfulTransaction();
    Bid existing = Bid.create(1L, 2L, Money.fromString("100.00"), NOW, "req-1").withId(5L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(liveLot));
    when(bidRepository.findByLotIdAndBidderIdAndClientRequestId(1L, 2L, "req-1"))
        .thenReturn(Optional.of(existing));

    service.placeBid(1L, 2L, Money.fromString("100.00"), "req-1");

    verify(domainEventPublisher, never()).publish(any());
  }

  @Test
  void rb05_duplicateClientRequestId_returnsOriginal() {
    stubSuccessfulTransaction();
    Bid existing = Bid.create(1L, 2L, Money.fromString("100.00"), NOW, "req-1").withId(5L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(liveLot));
    when(bidRepository.findByLotIdAndBidderIdAndClientRequestId(1L, 2L, "req-1"))
        .thenReturn(Optional.of(existing));

    var outcome = service.placeBid(1L, 2L, Money.fromString("100.00"), "req-1");

    assertThat(outcome.idempotentReplay()).isTrue();
    assertThat(outcome.result().bid().id()).isEqualTo(5L);
    verify(bidRepository, never()).save(any());
    verify(lotRepository, never()).save(any());
  }

  @Test
  void rb05_uniqueViolation_returnsOriginal() {
    stubSuccessfulTransaction();
    Bid existing = Bid.create(1L, 2L, Money.fromString("100.00"), NOW, "req-1").withId(7L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(liveLot));
    when(bidRepository.findByLotIdAndBidderIdAndClientRequestId(1L, 2L, "req-1"))
        .thenReturn(Optional.empty(), Optional.of(existing));
    when(bidRepository.save(any())).thenThrow(new DataIntegrityViolationException("unique"));
    when(lotRepository.findById(1L)).thenReturn(Optional.of(liveLot));

    var outcome = service.placeBid(1L, 2L, Money.fromString("100.00"), "req-1");

    assertThat(outcome.idempotentReplay()).isTrue();
    assertThat(outcome.result().bid().id()).isEqualTo(7L);
  }

  @Test
  void rb08_optimisticLockExhausted_throwsConflict() {
    when(transactionTemplate.execute(any()))
        .thenThrow(new OptimisticLockingFailureException("v1"))
        .thenThrow(new OptimisticLockingFailureException("v2"))
        .thenThrow(new OptimisticLockingFailureException("v3"));

    assertThatThrownBy(() -> service.placeBid(1L, 2L, Money.fromString("105.00"), "req-1"))
        .isInstanceOf(BidConflictException.class);
  }

  private Lot liveLot(Money currentPrice, int bidCount) {
    Instant end = NOW.plusSeconds(3600);
    return new Lot(
        1L,
        10L,
        "Title",
        "Desc",
        1L,
        Money.fromString("100.00"),
        Money.fromString("5.00"),
        null,
        LotStatus.LIVE,
        NOW.minusSeconds(3600),
        end,
        null,
        currentPrice,
        bidCount,
        null,
        0,
        0L,
        NOW,
        NOW);
  }

  private static User user(long id) {
    return new User(id, "u@test.com", "Bidder", java.util.Set.of(Role.BUYER), true);
  }
}
