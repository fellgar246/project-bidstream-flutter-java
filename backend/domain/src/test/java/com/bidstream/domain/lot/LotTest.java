package com.bidstream.domain.lot;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidstream.domain.money.Money;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class LotTest {

  private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");

  @Test
  void createDraftAndSchedule() {
    Lot draft = sampleDraft(null);
    Instant start = NOW.plus(10, ChronoUnit.MINUTES);
    Instant end = start.plus(1, ChronoUnit.HOURS);

    Lot scheduled = draft.withId(1L).schedule(start, end, NOW);

    assertThat(scheduled.status()).isEqualTo(LotStatus.SCHEDULED);
    assertThat(scheduled.scheduledStartAt()).isEqualTo(start);
  }

  @Test
  void cancelFromDraft() {
    Lot cancelled = sampleDraft(null).withId(1L).cancel(NOW);
    assertThat(cancelled.status()).isEqualTo(LotStatus.CANCELLED);
  }

  @Test
  void startFromScheduled() {
    Lot scheduled =
        sampleDraft(null)
            .withId(1L)
            .schedule(NOW.plus(10, ChronoUnit.MINUTES), NOW.plus(70, ChronoUnit.MINUTES), NOW);
    Lot live = scheduled.start(NOW);
    assertThat(live.status()).isEqualTo(LotStatus.LIVE);
  }

  @Test
  void closeSoldAndNoSale() {
    Lot live =
        sampleDraft(null)
            .withId(1L)
            .schedule(NOW.plus(10, ChronoUnit.MINUTES), NOW.plus(70, ChronoUnit.MINUTES), NOW)
            .start(NOW);

    Lot sold = live.closeSold(42L, NOW);
    assertThat(sold.status()).isEqualTo(LotStatus.CLOSED_SOLD);
    assertThat(sold.actualEndAt()).isEqualTo(NOW);
    assertThat(sold.winningBidId()).isEqualTo(42L);

    Lot noSale = live.closeNoSale(NOW);
    assertThat(noSale.status()).isEqualTo(LotStatus.CLOSED_NO_SALE);
  }

  @Test
  void updateDraftKeepsStatus() {
    Lot updated =
        sampleDraft(Money.fromCents(15000))
            .withId(1L)
            .updateDraft(
                "New title",
                "New desc",
                2L,
                Money.fromCents(20000),
                Money.fromCents(1000),
                Money.fromCents(20000),
                NOW);

    assertThat(updated.title()).isEqualTo("New title");
    assertThat(updated.hasReserve()).isTrue();
    assertThat(updated.isReserveMet()).isFalse();
  }

  @Test
  void reserveMetWhenCurrentPriceCoversReserve() {
    Lot lot =
        new Lot(
            1L,
            1L,
            "Title",
            "Desc",
            1L,
            Money.fromCents(10000),
            Money.fromCents(500),
            Money.fromCents(15000),
            LotStatus.LIVE,
            NOW,
            NOW.plus(1, ChronoUnit.HOURS),
            null,
            Money.fromCents(16000),
            2,
            null,
            0,
            0L,
            NOW,
            NOW);

    assertThat(lot.isReserveMet()).isTrue();
  }

  private Lot sampleDraft(Money reserve) {
    return Lot.createDraft(
        1L, "Title", "Description", 1L, Money.fromCents(10000), Money.fromCents(500), reserve, NOW);
  }
}
