package com.bidstream.api.lot;

public record LotImageResponse(
    long id, String url, String thumbnailUrl, int position, String contentType, String status) {}
