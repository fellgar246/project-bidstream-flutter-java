package com.bidstream.application.cache;

import java.util.List;

/**
 * Lot DTO cached in Redis without user-dependent fields ({@code watched}, {@code canEdit}, {@code
 * canBid}).
 */
public record CachedLotSnapshot(
    long id,
    String title,
    String description,
    CachedCategory category,
    CachedSeller seller,
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
    List<CachedImage> images,
    Double rank) {

  public record CachedCategory(long id, String name) {}

  public record CachedSeller(long id, String displayName) {}

  public record CachedImage(
      long id, String url, String thumbnailUrl, int position, String contentType, String status) {}
}
