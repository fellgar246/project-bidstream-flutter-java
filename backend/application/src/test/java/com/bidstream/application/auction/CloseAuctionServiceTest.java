package com.bidstream.application.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidstream.application.outbox.OutboxWriter;
import com.bidstream.application.realtime.DomainEventPublisher;
import com.bidstream.domain.bid.Bid;
import com.bidstream.domain.bid.BidRepository;
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

@ExtendWith(MockitoExtension.class)
class CloseAuctionServiceTest {

  private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

  @Mock private LotRepository lotRepository;
  @Mock private BidRepository bidRepository;
  @Mock private OutboxWriter outboxWriter;
  @Mock private DomainEventPublisher domainEventPublisher;
  @Mock private com.bidstream.application.cache.LotCacheInvalidator lotCacheInvalidator;
  @Mock private com.bidstream.application.metrics.BidstreamMetrics metrics;

  private CloseAuctionService service;
  private Lot liveLot;

  @BeforeEach
  void setUp() {
    service =
        new CloseAuctionService(
            lotRepository,
            bidRepository,
            outboxWriter,
            domainEventPublisher,
            Clock.fixed(NOW, ZoneOffset.UTC),
            lotCacheInvalidator,
            metrics);
    liveLot = sampleLiveLot(0, Money.fromCents(0));
  }

  @Test
  void close_noBids_emitsNoSaleWithReasonNoBids() {
    when(lotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(liveLot));
    when(lotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Lot result = service.closeLot(1L);

    assertThat(result.status()).isEqualTo(LotStatus.CLOSED_NO_SALE);
    assertThat(result.winningBidId()).isNull();
    verify(outboxWriter).writeLotClosedNoSale(result, CloseAuctionService.REASON_NO_BIDS, NOW);
  }

  @Test
  void close_reserveNotMet_emitsNoSaleWithoutWinner() {
    Lot withBids =
        new Lot(
            1L,
            10L,
            "Title",
            "Desc",
            1L,
            Money.fromCents(10000),
            Money.fromCents(500),
            Money.fromCents(20000),
            LotStatus.LIVE,
            NOW.minusSeconds(3600),
            NOW.plusSeconds(60),
            null,
            Money.fromCents(15000),
            2,
            null,
            0,
            0L,
            NOW,
            NOW);
    when(lotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(withBids));
    when(lotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Lot result = service.closeLot(1L);

    assertThat(result.status()).isEqualTo(LotStatus.CLOSED_NO_SALE);
    assertThat(result.winningBidId()).isNull();
    verify(outboxWriter)
        .writeLotClosedNoSale(result, CloseAuctionService.REASON_RESERVE_NOT_MET, NOW);
  }

  @Test
  void close_reserveMet_soldWithWinningBid() {
    Lot withBids = sampleLiveLot(3, Money.fromCents(25000));
    Bid winning = new Bid(99L, 1L, 42L, Money.fromCents(25000), NOW, "req-1", NOW);
    when(lotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(withBids));
    when(bidRepository.findHighestBidByLotId(1L)).thenReturn(Optional.of(winning));
    when(lotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Lot result = service.closeLot(1L);

    assertThat(result.status()).isEqualTo(LotStatus.CLOSED_SOLD);
    assertThat(result.winningBidId()).isEqualTo(99L);
    verify(outboxWriter).writeLotClosedSold(result, 99L, 42L, 25000L, NOW);
  }

  @Test
  void close_noReserveWithBids_sold() {
    Lot withBids = sampleLiveLot(1, Money.fromCents(12000));
    Bid winning = new Bid(5L, 1L, 7L, Money.fromCents(12000), NOW, "req-2", NOW);
    when(lotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(withBids));
    when(bidRepository.findHighestBidByLotId(1L)).thenReturn(Optional.of(winning));
    when(lotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Lot result = service.closeLot(1L);

    assertThat(result.status()).isEqualTo(LotStatus.CLOSED_SOLD);
    verify(outboxWriter).writeLotClosedSold(result, 5L, 7L, 12000L, NOW);
  }

  private Lot sampleLiveLot(int bidCount, Money currentPrice) {
    return new Lot(
        1L,
        10L,
        "Title",
        "Desc",
        1L,
        Money.fromCents(10000),
        Money.fromCents(500),
        null,
        LotStatus.LIVE,
        NOW.minusSeconds(3600),
        NOW.plusSeconds(60),
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
