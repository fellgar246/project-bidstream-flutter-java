package com.bidstream.application.bid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
            Clock.fixed(NOW, ZoneOffset.UTC));

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
    doThrow(new BidSelfException()).when(bidValidator).validate(liveLot, 10L, Money.fromString("105.00"), NOW);

    assertThatThrownBy(
            () -> service.placeBid(1L, 10L, Money.fromString("105.00"), "req-1"))
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
    doThrow(new BidNotLiveException()).when(bidValidator).validate(scheduled, 2L, Money.fromString("100.00"), NOW);

    assertThatThrownBy(
            () -> service.placeBid(1L, 2L, Money.fromString("100.00"), "req-1"))
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

    var outcome = service.placeBid(1L, 2L, Money.fromString("100.00"), "req-1");

    assertThat(outcome.idempotentReplay()).isFalse();
    assertThat(outcome.result().bid().id()).isEqualTo(1L);
    assertThat(outcome.result().lot().currentPrice()).isEqualTo(Money.fromString("100.00"));
    assertThat(outcome.result().lot().bidCount()).isEqualTo(1);
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
}
