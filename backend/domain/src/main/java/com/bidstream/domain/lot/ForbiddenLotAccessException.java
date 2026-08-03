package com.bidstream.domain.lot;

public class ForbiddenLotAccessException extends RuntimeException {

  public ForbiddenLotAccessException(String message) {
    super(message);
  }
}
