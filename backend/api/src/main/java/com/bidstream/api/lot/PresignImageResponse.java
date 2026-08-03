package com.bidstream.api.lot;

public record PresignImageResponse(
    long imageId, String uploadUrl, String storageKey, int expiresIn) {}
