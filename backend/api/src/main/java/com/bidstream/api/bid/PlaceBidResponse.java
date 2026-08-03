package com.bidstream.api.bid;

public record PlaceBidResponse(
    BidSummary bid, PlaceBidLotSummary lot, boolean youAreHighestBidder) {}
