package com.bidstream.api.lot;

public record LotImageSummary(
    long id, String url, String thumbnailUrl, int position, String contentType, String status) {}
