package com.bidstream.application.lot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bidstream.application.cache.CachedLotSnapshot;
import com.bidstream.application.cache.CachedLotSnapshotBuilder;
import com.bidstream.application.cache.LotReadCacheService;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotQuery;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotSort;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.money.Money;
import com.bidstream.domain.user.Role;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LotUseCasesTest {

  @Test
  void useCasesDelegateToLotService() {
    LotService lotService = mock(LotService.class);
    Lot lot =
        Lot.createDraft(
            1L, "T", "D", 1L, Money.fromCents(1000), Money.fromCents(100), null, Instant.now());

    when(lotService.createLot(1L, "T", "D", 1L, lot.startingPrice(), lot.minIncrement(), null))
        .thenReturn(lot);
    LotReadCacheService lotReadCacheService = mock(LotReadCacheService.class);
    CachedLotSnapshotBuilder snapshotBuilder = mock(CachedLotSnapshotBuilder.class);
    LotRepository lotRepository = mock(LotRepository.class);
    CachedLotSnapshot snapshot = mock(CachedLotSnapshot.class);

    when(lotReadCacheService.getLotSnapshot(1L)).thenReturn(snapshot);
    when(lotService.listPublicLots(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new com.bidstream.domain.lot.LotPage(List.of(lot), 0, 20, 1));
    when(lotReadCacheService.getLotPage(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            new com.bidstream.application.cache.CachedLotPageSnapshot(List.of(snapshot), 0, 20, 1));

    assertThat(
            new CreateLotUseCase(lotService)
                .execute(1L, "T", "D", 1L, lot.startingPrice(), lot.minIncrement(), null))
        .isEqualTo(lot);
    assertThat(new GetLotUseCase(lotReadCacheService).execute(1L)).isEqualTo(snapshot);
    assertThat(
            new ListLotsUseCase(lotService, lotRepository, lotReadCacheService, snapshotBuilder)
                .execute(sampleQuery())
                .content())
        .hasSize(1);
    when(lotService.listSellerLots(1L)).thenReturn(List.of(lot));
    when(lotService.listWatchlist(1L, 0, 20))
        .thenReturn(new com.bidstream.domain.lot.LotPage(List.of(lot), 0, 20, 1));
    assertThat(new ListMyLotsUseCase(lotService).execute(1L)).hasSize(1);
    assertThat(new ListWatchlistUseCase(lotService).execute(1L, 0, 20).content()).hasSize(1);

    new WatchLotUseCase(lotService).execute(1L, 1L);
    new UnwatchLotUseCase(lotService).execute(1L, 1L);
    new DeleteLotUseCase(lotService).execute(1L, EnumSet.of(Role.SELLER), 1L);
    new CancelLotUseCase(lotService).execute(1L, EnumSet.of(Role.SELLER), 1L);
    new ScheduleLotUseCase(lotService)
        .execute(1L, EnumSet.of(Role.SELLER), 1L, Instant.now(), Instant.now().plusSeconds(3600));
    new UpdateLotUseCase(lotService)
        .execute(
            1L,
            EnumSet.of(Role.SELLER),
            1L,
            "T",
            "D",
            1L,
            lot.startingPrice(),
            lot.minIncrement(),
            null);
    when(lotService.forceStatus(1L, LotStatus.LIVE)).thenReturn(lot);
    assertThat(new ForceLotStatusUseCase(lotService).execute(1L, LotStatus.LIVE)).isEqualTo(lot);
  }

  private LotQuery sampleQuery() {
    return new LotQuery(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        0,
        20,
        LotSort.ENDING_SOON);
  }
}
