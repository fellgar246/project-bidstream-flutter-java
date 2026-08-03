package com.bidstream.domain.bid;

import com.bidstream.domain.money.Money;
import java.time.Instant;
import java.util.Objects;

public record Bid(
    long id,
    long lotId,
    long bidderId,
    Money amount,
    Instant placedAt,
    String clientRequestId,
    Instant createdAt) {

  public Bid {
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(placedAt, "placedAt");
    Objects.requireNonNull(clientRequestId, "clientRequestId");
    if (clientRequestId.isBlank()) {
      throw new IllegalArgumentException("clientRequestId must not be blank");
    }
    if (amount.cents() <= 0) {
      throw new IllegalArgumentException("amount must be positive");
    }
  }

  public static Bid create(
      long lotId, long bidderId, Money amount, Instant placedAt, String clientRequestId) {
    return new Bid(0L, lotId, bidderId, amount, placedAt, clientRequestId, placedAt);
  }

  public Bid withId(long newId) {
    return new Bid(newId, lotId, bidderId, amount, placedAt, clientRequestId, createdAt);
  }
}
