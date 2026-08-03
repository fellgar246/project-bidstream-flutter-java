package com.bidstream.domain.lot;

import java.util.Map;

public class LotValidationException extends RuntimeException {

  private final Map<String, String> details;

  public LotValidationException(String message, Map<String, String> details) {
    super(message);
    this.details = Map.copyOf(details);
  }

  public Map<String, String> details() {
    return details;
  }
}
