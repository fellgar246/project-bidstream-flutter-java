package com.bidstream.domain.bid;

public class BidConflictException extends RuntimeException {

  public BidConflictException() {
    super("Bid could not be placed due to concurrent updates");
  }
}
