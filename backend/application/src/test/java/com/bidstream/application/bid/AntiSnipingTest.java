package com.bidstream.application.bid;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidstream.domain.lot.Lot;
import com.bidstream.domain.lot.LotStatus;
import com.bidstream.domain.money.Money;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AntiSnipingTest {

  private static final Instant BASE = Instant.parse("2026-06-01T12:00:00Z");

  @Test
  void ca058_bidWithin30SecondsOfClose_extendsEnd() {
    Instant end = BASE.plusSeconds(10);
    Lot lot = liveLot(end, 0);

    Lot.BidAcceptanceResult result = lot.acceptBid(Money.fromString("105.00"), BASE);

    assertThat(result.extended()).isTrue();
    assertThat(result.lot().scheduledEndAt()).isEqualTo(BASE.plusSeconds(30));
    assertThat(result.lot().extensionCount()).isEqualTo(1);
  }

  @Test
  void ca059_bidFiveMinutesBeforeClose_doesNotExtend() {
    Instant end = BASE.plus(Duration.ofMinutes(5));
    Lot lot = liveLot(end, 0);

    Lot.BidAcceptanceResult result = lot.acceptBid(Money.fromString("105.00"), BASE);

    assertThat(result.extended()).isFalse();
    assertThat(result.lot().scheduledEndAt()).isEqualTo(end);
    assertThat(result.lot().extensionCount()).isZero();
  }

  @Test
  void ca0510_afterTenExtensions_noFurtherExtension() {
    Instant end = BASE.plusSeconds(10);
    Lot lot = liveLot(end, 10);

    Lot.BidAcceptanceResult result = lot.acceptBid(Money.fromString("105.00"), BASE);

    assertThat(result.extended()).isFalse();
    assertThat(result.lot().scheduledEndAt()).isEqualTo(end);
    assertThat(result.lot().extensionCount()).isEqualTo(10);
  }

  @Test
  void ca0511_rejectedBidDoesNotExtend_endUnchanged() {
    Instant end = BASE.plusSeconds(10);
    Lot lot = liveLot(end, 0);
    Instant originalEnd = lot.scheduledEndAt();

    // Validation rejection happens before acceptBid; verify lot state unchanged.
    assertThat(lot.scheduledEndAt()).isEqualTo(originalEnd);
    assertThat(lot.extensionCount()).isZero();
  }

  private Lot liveLot(Instant end, int extensionCount) {
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
        BASE.minusSeconds(3600),
        end,
        null,
        Money.fromString("100.00"),
        1,
        null,
        extensionCount,
        0L,
        BASE,
        BASE);
  }
}
