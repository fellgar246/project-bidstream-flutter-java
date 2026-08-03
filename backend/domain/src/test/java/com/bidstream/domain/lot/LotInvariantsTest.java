package com.bidstream.domain.lot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidstream.domain.money.Money;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class LotInvariantsTest {

  private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");

  @Test
  void startingPriceMustBePositive() {
    assertThatThrownBy(
            () ->
                Lot.createDraft(
                    1L, "Title", "Desc", 1L, Money.fromCents(0), Money.fromCents(100), null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void minIncrementMustBePositive() {
    assertThatThrownBy(
            () ->
                Lot.createDraft(
                    1L, "Title", "Desc", 1L, Money.fromCents(100), Money.fromCents(0), null, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reservePriceMustBeAtLeastStartingPrice() {
    assertThatThrownBy(
            () ->
                Lot.createDraft(
                    1L,
                    "Title",
                    "Desc",
                    1L,
                    Money.fromCents(1000),
                    Money.fromCents(100),
                    Money.fromCents(500),
                    NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void scheduleEndMustBeAfterStart() {
    Lot draft = sampleDraft();
    Instant start = NOW.plus(10, ChronoUnit.MINUTES);
    Instant end = start.minus(1, ChronoUnit.MINUTES);

    assertThatThrownBy(() -> draft.schedule(start, end, NOW))
        .isInstanceOf(LotValidationException.class)
        .satisfies(
            ex ->
                assertThat(((LotValidationException) ex).details()).containsKey("scheduledEndAt"));
  }

  @Test
  void scheduleDurationMustBeAtLeastFiveMinutes() {
    Lot draft = sampleDraft();
    Instant start = NOW.plus(10, ChronoUnit.MINUTES);
    Instant end = start.plus(4, ChronoUnit.MINUTES);

    assertThatThrownBy(() -> draft.schedule(start, end, NOW))
        .isInstanceOf(LotValidationException.class);
  }

  @Test
  void scheduleDurationMustNotExceedFourteenDays() {
    Lot draft = sampleDraft();
    Instant start = NOW.plus(10, ChronoUnit.MINUTES);
    Instant end = start.plus(15, ChronoUnit.DAYS);

    assertThatThrownBy(() -> draft.schedule(start, end, NOW))
        .isInstanceOf(LotValidationException.class);
  }

  @Test
  void scheduleStartMustBeAtLeastFiveMinutesInFuture() {
    Lot draft = sampleDraft();
    Instant start = NOW.plus(4, ChronoUnit.MINUTES);
    Instant end = start.plus(1, ChronoUnit.HOURS);

    assertThatThrownBy(() -> draft.schedule(start, end, NOW))
        .isInstanceOf(LotValidationException.class)
        .satisfies(
            ex ->
                assertThat(((LotValidationException) ex).details())
                    .containsKey("scheduledStartAt"));
  }

  @Test
  void currentPriceNeverDecreases_isEnforcedByImmutability() {
    Lot draft = sampleDraft();
    assertThat(draft.currentPrice()).isEqualTo(Money.fromCents(0));
  }

  private Lot sampleDraft() {
    return Lot.createDraft(
        1L,
        "Vintage watch",
        "Description",
        1L,
        Money.fromCents(10000),
        Money.fromCents(500),
        null,
        NOW);
  }
}
