package com.bidstream.domain.bid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidstream.domain.money.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BidTest {

  @Test
  void create_setsFields() {
    Instant now = Instant.parse("2026-01-01T12:00:00Z");
    Bid bid = Bid.create(1L, 2L, Money.fromString("100.00"), now, "req-1");

    assertThat(bid.lotId()).isEqualTo(1L);
    assertThat(bid.bidderId()).isEqualTo(2L);
    assertThat(bid.amount()).isEqualTo(Money.fromString("100.00"));
    assertThat(bid.clientRequestId()).isEqualTo("req-1");
    assertThat(bid.id()).isZero();
  }

  @Test
  void rejectsBlankClientRequestId() {
    assertThatThrownBy(
            () -> Bid.create(1L, 2L, Money.fromString("10.00"), Instant.now(), "  "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonPositiveAmount() {
    assertThatThrownBy(
            () -> Bid.create(1L, 2L, Money.fromCents(0), Instant.now(), "req-1"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void withId_returnsCopyWithId() {
    Bid bid = Bid.create(1L, 2L, Money.fromString("50.00"), Instant.now(), "req-1");
    Bid withId = bid.withId(99L);
    assertThat(withId.id()).isEqualTo(99L);
    assertThat(withId.amount()).isEqualTo(bid.amount());
  }
}
