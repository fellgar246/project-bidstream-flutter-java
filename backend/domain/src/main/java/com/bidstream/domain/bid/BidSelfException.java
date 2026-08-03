package com.bidstream.domain.bid;

public class BidSelfException extends RuntimeException {

  public BidSelfException() {
    super("Seller cannot bid on their own lot");
  }
}
