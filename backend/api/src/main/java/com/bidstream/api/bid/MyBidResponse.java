package com.bidstream.api.bid;

public record MyBidResponse(
    BidSummary bid,
    long lotId,
    String lotTitle,
    String lotStatus,
    String lotCurrentPrice,
    String lotScheduledEndAt) {}
