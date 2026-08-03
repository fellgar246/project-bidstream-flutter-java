package com.bidstream.application.lot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotPage;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.lot.Watch;
import com.bidstream.domain.lot.WatchRepository;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LotServiceExtendedTest {

  private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");

  @Mock private LotRepository lotRepository;
  @Mock private WatchRepository watchRepository;

  private LotService lotService;

  @BeforeEach
  void setUp() {
    lotService = new LotService(lotRepository, watchRepository, new LotsProperties(), CLOCK);
  }

  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void createLot_persistsDraft() {
    when(lotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Lot lot =
        lotService.createLot(
            7L, "Title", "Desc", 1L, Money.fromString("10.00"), Money.fromString("1.00"), null);

    assertThat(lot.status()).isEqualTo(LotStatus.DRAFT);
    verify(lotRepository).save(any());
  }

  @Test
  void watchLot_isIdempotent() {
    Lot lot = draft(1L, 7L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
    when(watchRepository.findByUserIdAndLotId(9L, 1L)).thenReturn(Optional.of(existingWatch()));

    lotService.watchLot(9L, 1L);

    verify(watchRepository).findByUserIdAndLotId(9L, 1L);
  }

  @Test
  void canEditAndCanBidFlags() {
    Lot draft = draft(1L, 7L);
    Lot live =
        draft
            .schedule(NOW.plus(10, ChronoUnit.MINUTES), NOW.plus(70, ChronoUnit.MINUTES), NOW)
            .start(NOW);

    assertThat(lotService.canEdit(draft, 7L, EnumSet.of(Role.SELLER))).isTrue();
    assertThat(lotService.canEdit(live, 7L, EnumSet.of(Role.SELLER))).isFalse();
    assertThat(lotService.canBid(live, 8L)).isTrue();
    assertThat(lotService.canBid(live, 7L)).isFalse();
  }

  @Test
  void listWatchlist_mapsLots() {
    when(watchRepository.findByUserId(9L, 0, 20)).thenReturn(List.of(existingWatch()));
    when(watchRepository.countByUserId(9L)).thenReturn(1L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(draft(1L, 7L)));

    LotPage page = lotService.listWatchlist(9L, 0, 20);

    assertThat(page.content()).hasSize(1);
  }

  @Test
  void scheduleUpdateCancelDeleteAndForceStatus() {
    Lot draft = draft(1L, 7L);
    when(lotRepository.findById(1L)).thenReturn(Optional.of(draft));
    when(lotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Instant start = NOW.plus(10, ChronoUnit.MINUTES);
    Instant end = start.plus(1, ChronoUnit.HOURS);

    Lot scheduled = lotService.scheduleLot(7L, EnumSet.of(Role.SELLER), 1L, start, end);
    assertThat(scheduled.status()).isEqualTo(LotStatus.SCHEDULED);

    Lot updated =
        lotService.updateLot(
            7L,
            EnumSet.of(Role.SELLER),
            1L,
            "New",
            "Desc",
            1L,
            Money.fromCents(2000),
            Money.fromCents(200),
            null);
    assertThat(updated.title()).isEqualTo("New");

    Lot cancelled = lotService.cancelLot(7L, EnumSet.of(Role.SELLER), 1L);
    assertThat(cancelled.status()).isEqualTo(LotStatus.CANCELLED);

    Lot forced = lotService.forceStatus(1L, LotStatus.LIVE);
    assertThat(forced.status()).isEqualTo(LotStatus.LIVE);

    lotService.unwatchLot(9L, 1L);
    verify(watchRepository).deleteByUserIdAndLotId(9L, 1L);

    when(lotRepository.findById(2L)).thenReturn(Optional.of(draft(2L, 7L)));
    lotService.deleteLot(7L, EnumSet.of(Role.SELLER), 2L);
    verify(lotRepository).deleteById(2L);
  }

  @Test
  void listPublicAndSellerLots() {
    when(lotRepository.findPublic(any())).thenReturn(new LotPage(List.of(draft(1L, 7L)), 0, 20, 1));
    when(lotRepository.findBySellerId(7L)).thenReturn(List.of(draft(1L, 7L)));

    assertThat(lotService.listPublicLots(anyQuery()).content()).hasSize(1);
    assertThat(lotService.listSellerLots(7L)).hasSize(1);
    assertThat(lotService.isWatched(9L, 1L)).isFalse();
  }

  private com.bidstream.domain.lot.LotQuery anyQuery() {
    return new com.bidstream.domain.lot.LotQuery(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        0,
        20,
        com.bidstream.domain.lot.LotSort.ENDING_SOON);
  }

  private Lot draft(long id, long sellerId) {
    return Lot.createDraft(
            sellerId, "Title", "Desc", 1L, Money.fromCents(1000), Money.fromCents(100), null, NOW)
        .withId(id);
  }

  private Watch existingWatch() {
    return new Watch(1L, 9L, 1L, NOW);
  }
}
