package com.bidstream.domain.bid;

public class BidNotLiveException extends RuntimeException {

  public BidNotLiveException() {
    super("Lot is not accepting bids");
  }
}
