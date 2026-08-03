package com.bidstream.application.bid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidstream.domain.bid.BidNotLiveException;
import com.bidstream.domain.bid.BidSelfException;
import com.bidstream.domain.bid.BidTooLowException;
import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.money.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BidValidatorTest {

  private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
  private final BidValidator validator = new BidValidator();

  @Test
  void rb04_minimumForFirstBid_isStartingPrice() {
    Lot lot = lot(Money.fromCents(0), 0, NOW.plusSeconds(3600));
    assertThat(lot.minimumNextBid()).isEqualTo(Money.fromString("100.00"));
  }

  @Test
  void rb04_minimumAfterBid_isCurrentPlusIncrement() {
    Lot lot = lot(Money.fromString("105.00"), 1, NOW.plusSeconds(3600));
    assertThat(lot.minimumNextBid()).isEqualTo(Money.fromString("110.00"));
  }

  @Test
  void rb04_pastScheduledEnd_rejects() {
    Lot lot = lot(Money.fromString("100.00"), 1, NOW.minusMillis(1));
    assertThatThrownBy(
            () -> validator.validate(lot, 2L, Money.fromString("110.00"), NOW))
        .isInstanceOf(BidNotLiveException.class);
  }

  @Test
  void rb04_sellerBid_rejects() {
    Lot lot = lot(Money.fromString("100.00"), 0, NOW.plusSeconds(3600));
    assertThatThrownBy(
            () -> validator.validate(lot, 10L, Money.fromString("100.00"), NOW))
        .isInstanceOf(BidSelfException.class);
  }

  @Test
  void rb04_tooLow_includesMinimum() {
    Lot lot = lot(Money.fromString("105.00"), 1, NOW.plusSeconds(3600));
    assertThatThrownBy(
            () -> validator.validate(lot, 2L, Money.fromString("106.00"), NOW))
        .isInstanceOf(BidTooLowException.class)
        .satisfies(
            ex -> assertThat(((BidTooLowException) ex).minimum())
                .isEqualTo(Money.fromString("110.00")));
  }

  private Lot lot(Money currentPrice, int bidCount, Instant end) {
    return new Lot(
        1L,
        10L,
        "T",
        "D",
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
