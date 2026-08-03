package com.bidstream.application.bid;

import com.bidstream.domain.bid.Bid;

public record PlaceBidResult(
    Bid bid, com.bidstream.domain.lot.Lot lot, boolean youAreHighestBidder, boolean extended) {

  public static PlaceBidResult of(
      Bid bid,
      com.bidstream.domain.lot.Lot lot,
      long bidderId,
      boolean extended,
      boolean idempotentReplay) {
    boolean highest = bid.bidderId() == bidderId;
    return new PlaceBidResult(bid, lot, highest, extended);
  }
}
