package com.bidstream.domain.bid;

public class LockTimeoutException extends RuntimeException {

  public LockTimeoutException() {
    super("Could not acquire distributed lock");
  }
}
