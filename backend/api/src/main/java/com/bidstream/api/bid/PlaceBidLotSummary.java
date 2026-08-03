package com.bidstream.api.bid;

public record PlaceBidLotSummary(
    String currentPrice, int bidCount, String scheduledEndAt, boolean extended) {}
