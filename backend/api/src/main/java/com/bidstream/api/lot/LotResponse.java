package com.bidstream.api.lot;

import java.util.List;

public record LotResponse(
    long id,
    String title,
    String description,
    CategorySummary category,
    SellerSummary seller,
    String startingPrice,
    String minIncrement,
    String currentPrice,
    int bidCount,
    boolean hasReserve,
    boolean reserveMet,
    String status,
    String scheduledStartAt,
    String scheduledEndAt,
    String actualEndAt,
    List<Object> images,
    boolean watched,
    boolean canEdit,
    boolean canBid) {}
