package com.bidstream.application.lot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotPage;
import com.bidstream.domain.lot.LotQuery;
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
    when(lotService.getLot(1L)).thenReturn(lot);
    when(lotService.listPublicLots(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new LotPage(List.of(lot), 0, 20, 1));
    when(lotService.listSellerLots(1L)).thenReturn(List.of(lot));
    when(lotService.listWatchlist(1L, 0, 20)).thenReturn(new LotPage(List.of(lot), 0, 20, 1));

    assertThat(
            new CreateLotUseCase(lotService)
                .execute(1L, "T", "D", 1L, lot.startingPrice(), lot.minIncrement(), null))
        .isEqualTo(lot);
    assertThat(new GetLotUseCase(lotService).execute(1L)).isEqualTo(lot);
    assertThat(new ListLotsUseCase(lotService).execute(sampleQuery()).content()).hasSize(1);
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
