package com.bidstream.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Value object representing monetary amounts in cents (RB-01). */
public final class Money implements Comparable<Money> {

  private final long cents;

  private Money(long cents) {
    this.cents = cents;
  }

  public static Money fromCents(long cents) {
    return new Money(cents);
  }

  public static Money fromString(String decimalAmount) {
    Objects.requireNonNull(decimalAmount, "decimalAmount");
    String normalized = decimalAmount.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Amount must not be blank");
    }
    BigDecimal value = new BigDecimal(normalized).setScale(2, RoundingMode.UNNECESSARY);
    return new Money(value.movePointRight(2).longValueExact());
  }

  public long cents() {
    return cents;
  }

  public Money add(Money other) {
    Objects.requireNonNull(other, "other");
    return new Money(this.cents + other.cents);
  }

  public Money subtract(Money other) {
    Objects.requireNonNull(other, "other");
    return new Money(this.cents - other.cents);
  }

  @Override
  public int compareTo(Money other) {
    return Long.compare(this.cents, other.cents);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Money money)) {
      return false;
    }
    return cents == money.cents;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(cents);
  }

  @Override
  public String toString() {
    BigDecimal value = BigDecimal.valueOf(cents, 2);
    return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
  }
}
