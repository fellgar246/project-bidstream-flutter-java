package com.bidstream.domain.bid;

import com.bidstream.domain.money.Money;
import java.util.Map;

public class BidTooLowException extends RuntimeException {

  private final Money minimum;

  public BidTooLowException(Money minimum) {
    super("Bid amount is too low");
    this.minimum = minimum;
  }

  public Money minimum() {
    return minimum;
  }

  public Map<String, String> details() {
    return Map.of("minimumCents", String.valueOf(minimum.cents()));
  }
}
